package de.aaaaaaah.velcom.backend.newaccess.taskaccess;

import static org.jooq.codegen.db.tables.Task.TASK;
import static org.jooq.impl.DSL.not;

import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.shared.util.Either;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jooq.codegen.db.tables.records.TaskRecord;

public class TaskWriteAccess extends TaskReadAccess {

	public TaskWriteAccess(DatabaseStorage databaseStorage) {
		super(databaseStorage);
	}

	private static TaskRecord taskToTaskRecord(Task task) {
		TaskRecord record = new TaskRecord();
		record.setId(task.getIdAsString());
		record.setAuthor(task.getAuthor());
		record.setPriority(task.getPriority().asInt());
		record.setInsertTime(task.getInsertTime());
		record.setUpdateTime(task.getUpdateTime());
		record.setRepoId(task.getRepoId().map(RepoId::getIdAsString).orElse(null));
		record.setCommitHash(task.getCommitHash().map(CommitHash::getHash).orElse(null));
		record.setDescription(task.getTarDescription().orElse(null));
		record.setInProcess(task.isInProgress());
		return record;
	}

	/**
	 * Insert a single commit if no corresponding task already exists and return the newly created
	 * task.
	 *
	 * @param author the new task's author
	 * @param repoId the commit's repo id
	 * @param hash the commit's hash
	 * @param priority the new task's priority
	 * @return the newly created task if no task corresponding to this commit already existed
	 */
	public Optional<Task> insertCommit(String author, RepoId repoId, CommitHash hash,
		TaskPriority priority) {

		return databaseStorage.acquireWriteTransaction(db -> {
			boolean taskAlreadyExists = db.selectFrom(TASK)
				.where(TASK.REPO_ID.eq(repoId.getIdAsString()))
				.and(TASK.COMMIT_HASH.eq(hash.getHash()))
				.fetchAny() != null;

			if (taskAlreadyExists) {
				return Optional.empty();
			}

			Task task = new Task(author, priority, Either.ofLeft(new CommitSource(repoId, hash)));
			db.dsl().batchInsert(taskToTaskRecord(task)).execute();
			return Optional.of(task);
		});
	}


	/**
	 * Insert all commits for which no corresponding task exists yet.
	 *
	 * @param author the new task's author
	 * @param repoId the commit's repo id
	 * @param hashes the commit hashes
	 * @param priority the new task's priority
	 */
	public void insertCommits(String author, RepoId repoId, Collection<CommitHash> hashes,
		TaskPriority priority) {

		Set<String> hashesAsStrings = hashes.stream()
			.map(CommitHash::getHash)
			.collect(Collectors.toSet());

		databaseStorage.acquireWriteTransaction(db -> {
			Set<String> hashesAlreadyInQueue = db.selectFrom(TASK)
				.where(TASK.REPO_ID.eq(repoId.getIdAsString()))
				.and(TASK.COMMIT_HASH.in(hashesAsStrings))
				.fetchSet(TASK.COMMIT_HASH);

			List<TaskRecord> tasksToInsert = hashes.stream()
				.filter(hash -> !hashesAlreadyInQueue.contains(hash.getHash()))
				.map(hash -> new CommitSource(repoId, hash))
				.map(source -> new Task(author, priority, Either.ofLeft(source)))
				.map(TaskWriteAccess::taskToTaskRecord)
				.collect(Collectors.toList());

			db.dsl().batchInsert(tasksToInsert).execute();
		});
	}

	/**
	 * Atomically select a task to start and start it via a custom selector function.
	 *
	 * @param selector The selector is passed a list of all tasks that haven't been started yet. The
	 * 	list is in no particular order. If it returns a task, that task will be started and also
	 * 	returned.
	 * @return the task returned by the selector, if it returned one
	 */
	public Optional<Task> startTask(Function<List<Task>, Optional<Task>> selector) {
		// TODO: 08.11.20 Return a non-outdated Task
		return databaseStorage.acquireWriteTransaction(db -> {
			List<Task> allTasks = db.selectFrom(TASK)
				.where(not(TASK.IN_PROCESS))
				.stream()
				.map(TaskReadAccess::taskRecordToTask)
				.collect(Collectors.toList());

			Optional<Task> taskToRemove = selector.apply(allTasks);

			taskToRemove.ifPresent(task -> db.update(TASK)
				.set(TASK.IN_PROCESS, true)
				.where(TASK.ID.eq(task.getIdAsString())));

			return taskToRemove;
		});
	}

	/**
	 * Delete the tasks with the specified ids.
	 *
	 * @param tasks the ids of the tasks to delete
	 */
	public void deleteTasks(Collection<TaskId> tasks) {
		Set<String> taskIds = tasks.stream()
			.map(TaskId::getIdAsString)
			.collect(Collectors.toSet());

		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.deleteFrom(TASK)
				.where(TASK.ID.in(taskIds))
				.execute();
		}
	}

	/**
	 * Delete all tasks.
	 */
	public void deleteAllTasks() {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.deleteFrom(TASK).execute();
		}
	}

	/**
	 * Set the priority of the task with the specified id, if such a task exists. Also changes the
	 * task's update time.
	 *
	 * @param taskId the task's id
	 * @param newPriority the task's new priority
	 */
	public void setTaskPriority(TaskId taskId, TaskPriority newPriority) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.update(TASK)
				.set(TASK.PRIORITY, newPriority.asInt())
				.set(TASK.UPDATE_TIME, Instant.now())
				.where(TASK.ID.eq(taskId.getIdAsString()))
				.execute();
		}
	}

	/**
	 * Sets the status of the task with the specified id, if such a task exists. Does not changes the
	 * task's update time.
	 *
	 * @param taskId the task's id
	 * @param inProcess the task's new status
	 */
	public void setTaskInProgress(TaskId taskId, boolean inProcess) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.update(TASK)
				.set(TASK.IN_PROCESS, inProcess)
				.where(TASK.ID.eq(taskId.getIdAsString()))
				.execute();
		}
	}

	/**
	 * Reset all tasks to the "not in progress" status.
	 */
	public void resetAllTaskStatuses() {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.update(TASK)
				.set(TASK.IN_PROCESS, false)
				.execute();
		}
	}
}
