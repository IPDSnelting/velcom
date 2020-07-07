package de.aaaaaaah.velcom.backend.access;

import static java.util.stream.Collectors.toList;
import static org.jooq.codegen.db.Tables.TASK;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.RepoSource;
import de.aaaaaaah.velcom.backend.access.entities.TarSource;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.policy.QueuePolicy;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.jooq.DSLContext;

public class TaskWriteAccess extends TaskReadAccess {

	private final Collection<Consumer<Task>> insertHandlers = new ArrayList<>();
	private final Collection<Consumer<TaskId>> deleteHandlers = new ArrayList<>();
	private QueuePolicy policy;

	public TaskWriteAccess(DatabaseStorage databaseStorage) {
		super(databaseStorage);

		// Reset "in process" status for all tasks
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.update(TASK).set(TASK.IN_PROCESS, false).execute();
		}
	}

	public void onTaskInsert(Consumer<Task> insertHandler) {
		this.insertHandlers.add(Objects.requireNonNull(insertHandler));
	}

	public void onTaskDelete(Consumer<TaskId> deleteHandler) {
		this.deleteHandlers.add(Objects.requireNonNull(deleteHandler));
	}

	public void insertTasks(Collection<Task> tasks) {
		insertTasks(tasks, databaseStorage.acquireContext());
	}

	void insertTasks(Collection<Task> tasks, DSLContext db) {
		// Get tasks that are already in queue
		List<String> idList = tasks.stream()
			.map(Task::getId)
			.map(TaskId::getId)
			.map(UUID::toString)
			.collect(toList());

		Set<String> existingIds = db.selectFrom(TASK).where(TASK.ID.in(idList)).fetchSet(TASK.ID);

		// Insert tasks that are not already in the queue
		var insert = db.insertInto(TASK)
			.columns(TASK.ID, TASK.AUTHOR, TASK.PRIORITY, TASK.TAR_NAME, TASK.COMMIT_HASH,
				TASK.REPO_ID);

		for (Task task : tasks) {
			if (existingIds.contains(task.getId().getId().toString())) {
				continue; // task is already in table => skip!
			}

			insert.values(
				task.getId().getId().toString(),
				task.getAuthor(),
				task.getPriority(),
				task.getSource().getRight().map(TarSource::getTarName).orElse(null),
				task.getSource()
					.getLeft()
					.map(RepoSource::getHash)
					.map(CommitHash::getHash)
					.orElse(null),
				task.getSource()
					.getLeft()
					.map(RepoSource::getRepoId)
					.map(RepoId::getId)
					.map(UUID::toString)
					.orElse(null)
			);
		}

		insert.execute();

		for (Consumer<Task> insertHandler : insertHandlers) {
			tasks.forEach(insertHandler);
		}
	}

	public void deleteTasks(Collection<TaskId> taskIds) {
		deleteTasks(taskIds, databaseStorage.acquireContext());
	}

	void deleteTasks(Collection<TaskId> taskIds, DSLContext db) {
		List<String> taskIdStrings = taskIds.stream()
			.map(TaskId::getId)
			.map(UUID::toString)
			.collect(toList());

		db.deleteFrom(TASK).where(TASK.ID.in(taskIdStrings)).execute();

		for (Consumer<TaskId> deleteHandler : deleteHandlers) {
			taskIds.forEach(deleteHandler);
		}
	}

	public void deleteAllTasks() {
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.deleteFrom(TASK).execute();
		}
	}

	public void deleteAllTasksOfRepo(RepoId repoId) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.deleteFrom(TASK).where(TASK.REPO_ID.eq(repoId.getId().toString())).execute();
		}
	}

	public void setTaskPriority(TaskId taskId, int newPriority) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.update(TASK)
				.set(TASK.PRIORITY, newPriority)
				.set(TASK.UPDATE_TIME, Timestamp.from(Instant.now()))
				.where(TASK.ID.eq(taskId.getId().toString()))
				.execute();
		}
	}

	public void setTaskStatus(TaskId taskId, boolean taskInProcess) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.update(TASK)
				.set(TASK.IN_PROCESS, taskInProcess)
				.where(TASK.ID.eq(taskId.getId().toString()))
			.execute();
		}
	}

}
