package de.aaaaaaah.velcom.backend.access;

import static java.util.stream.Collectors.toList;
import static org.jooq.codegen.db.Tables.TASK;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.CommitId;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.access.policy.QueuePriority;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.jooq.Condition;
import org.jooq.impl.DSL;

/**
 * Provides write access to task objects stored in a database.
 */
public class TaskWriteAccess extends TaskReadAccess {

	private final Collection<Consumer<Task>> insertHandlers = new ArrayList<>();
	private final Collection<Consumer<TaskId>> deleteHandlers = new ArrayList<>();

	public TaskWriteAccess(DatabaseStorage databaseStorage) {
		super(databaseStorage);

		// Reset "in process" status for all tasks
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.update(TASK).set(TASK.IN_PROCESS, false).execute();
		}
	}

	/**
	 * Adds a handler that is called when a new task is inserted.
	 *
	 * @param insertHandler the handler
	 */
	public void onTaskInsert(Consumer<Task> insertHandler) {
		this.insertHandlers.add(Objects.requireNonNull(insertHandler));
	}

	/**
	 * Adds a new handler which is called when a task is deleted.
	 *
	 * @param deleteHandler the handler
	 */
	public void onTaskDelete(Consumer<TaskId> deleteHandler) {
		this.deleteHandlers.add(Objects.requireNonNull(deleteHandler));
	}

	/**
	 * Inserts the given tasks into the database.
	 *
	 * @param tasks the tasks to insert
	 * @return a collection of all tasks that were actually inserted because they were not already in
	 * 	the database beforehand
	 */
	public Collection<Task> insertTasks(Collection<Task> tasks) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			return insertTasks(tasks, db);
		}
	}

	/**
	 * Inserts the given tasks into the database by using the provided write access.
	 *
	 * @param originalTasks the tasks to insert
	 * @param db the access that will be used
	 * @return a collection of all tasks that were actually inserted because they were not already in
	 * 	the database beforehand
	 */
	Collection<Task> insertTasks(Collection<Task> originalTasks, DBWriteAccess db) {
		// Create mutable copy of task list
		final List<Task> tasks = new ArrayList<>(originalTasks);

		// 1.) Get tasks that are already in queue
		List<String> idList = tasks.stream()
			.map(Task::getId)
			.map(TaskId::getId)
			.map(UUID::toString)
			.collect(toList());

		List<CommitId> commitIdList = tasks.stream()
			.filter(task -> task.getSource().isLeft())
			.map(task -> task.getSource().getLeft().get())
			.map(source -> new CommitId(source.getRepoId(), source.getHash()))
			.collect(toList());

		Condition condition = DSL.noCondition();

		for (CommitId commitId : commitIdList) {
			Condition subCondition = TASK.REPO_ID.eq(commitId.getRepoId().getId().toString())
				.and(TASK.COMMIT_HASH.eq(commitId.getHash().getHash()));

			condition = condition.or(subCondition);
		}

		final Set<TaskId> inDatabaseTaskIds = new HashSet<>();
		final Set<CommitSource> inDatabaseTaskCommitIds = new HashSet<>();
		final Condition finalCondition = condition;

		db.transaction(transaction -> {
			transaction.selectFrom(TASK)
				.where(TASK.ID.in(idList).or(finalCondition))
				.fetch()
				.forEach(taskRecord -> {
					inDatabaseTaskIds.add(new TaskId(UUID.fromString(taskRecord.getId())));
					if (taskRecord.getCommitHash() != null) {
						inDatabaseTaskCommitIds.add(new CommitSource(
							new RepoId(UUID.fromString(taskRecord.getRepoId())),
							new CommitHash(taskRecord.getCommitHash())
						));
					}
				});

			// 2.) Remove all tasks from list that are already in database
			tasks.removeIf(task -> inDatabaseTaskIds.contains(task.getId()));
			tasks.removeIf(task -> task.getSource().isLeft()
				&& inDatabaseTaskCommitIds.contains(task.getSource().getLeft().get()));

			// 3.) Insert remaining tasks
			if (!tasks.isEmpty()) {
				var insert = transaction.insertInto(TASK)
					.columns(TASK.ID, TASK.AUTHOR, TASK.PRIORITY, TASK.INSERT_TIME,
						TASK.UPDATE_TIME, TASK.DESCRIPTION, TASK.REPO_ID, TASK.COMMIT_HASH);

				for (Task task : tasks) {
					insert.values(
						task.getId().getId().toString(),
						task.getAuthor(),
						task.getPriority(),
						Timestamp.from(task.getInsertTime()),
						Timestamp.from(task.getUpdateTime()),
						task.getSource().getRight().map(TarSource::getDescription).orElse(null),
						task.getRepoId().map(RepoId::getId).map(UUID::toString).orElse(null),
						task.getSource()
							.getLeft()
							.map(CommitSource::getHash)
							.map(CommitHash::getHash)
							.orElse(null)
					);
				}

				insert.execute();
			}
		});

		// 4.) Call listeners
		for (Consumer<Task> insertHandler : insertHandlers) {
			tasks.forEach(insertHandler);
		}

		return tasks;
	}

	/**
	 * Deletes all tasks whose ids match the given task ids.
	 *
	 * @param taskIds the ids of the tasks to delete
	 */
	public void deleteTasks(Collection<TaskId> taskIds) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			deleteTasks(taskIds, db);
		}
	}

	void deleteTasks(Collection<TaskId> taskIds, DBWriteAccess db) {
		List<String> taskIdStrings = taskIds.stream()
			.map(TaskId::getId)
			.map(UUID::toString)
			.collect(toList());

		db.deleteFrom(TASK).where(TASK.ID.in(taskIdStrings)).execute();

		for (Consumer<TaskId> deleteHandler : deleteHandlers) {
			taskIds.forEach(deleteHandler);
		}
	}

	/**
	 * Deletes all tasks.
	 */
	public void deleteAllTasks() {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.deleteFrom(TASK).execute();
		}
	}

	/**
	 * Deletes all tasks that are associated with the given repo id.
	 *
	 * @param repoId the repo id
	 */
	public void deleteAllTasksOfRepo(RepoId repoId) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.deleteFrom(TASK).where(TASK.REPO_ID.eq(repoId.getId().toString())).execute();
		}
	}

	public void setTaskPriority(TaskId taskId, QueuePriority newPriority) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.update(TASK)
				.set(TASK.PRIORITY, newPriority.getAsInt())
				.set(TASK.UPDATE_TIME, Timestamp.from(Instant.now()))
				.where(TASK.ID.eq(taskId.getId().toString()))
				.execute();
		}
	}

	public void setTaskStatus(TaskId taskId, boolean taskInProcess) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.update(TASK)
				.set(TASK.IN_PROCESS, taskInProcess)
				.where(TASK.ID.eq(taskId.getId().toString()))
				.execute();
		}
	}

}
