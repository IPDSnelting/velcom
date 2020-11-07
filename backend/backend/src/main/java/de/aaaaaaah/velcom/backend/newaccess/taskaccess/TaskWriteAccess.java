package de.aaaaaaah.velcom.backend.newaccess.taskaccess;

import static org.jooq.codegen.db.tables.Task.TASK;

import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.shared.TaskCallbacks;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jooq.codegen.db.tables.records.TaskRecord;

public class TaskWriteAccess extends TaskReadAccess {

	private final TaskCallbacks taskCallbacks;

	public TaskWriteAccess(DatabaseStorage databaseStorage, TaskCallbacks taskCallbacks) {
		super(databaseStorage);
		this.taskCallbacks = taskCallbacks;
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
		record.setInProcess(task.isInProcess());
		return record;
	}

	/**
	 * Insert all tasks into the db that aren't already in the db.
	 *
	 * @param tasks the tasks to insert
	 * @return the tasks which were actually inserted
	 */
	public List<Task> insertOrIgnoreTasks(Collection<Task> tasks) {
		List<Task> insertedTasks = databaseStorage.acquireWriteTransaction(db -> {
			Set<String> taskIdsInQueue = db.selectFrom(TASK)
				.fetchSet(TASK.ID);

			List<Task> tasksToInsert = tasks.stream()
				.filter(task -> !taskIdsInQueue.contains(task.getIdAsString()))
				.collect(Collectors.toList());

			List<TaskRecord> taskRecordsToInsert = tasksToInsert.stream()
				.map(TaskWriteAccess::taskToTaskRecord)
				.collect(Collectors.toList());

			db.dsl().batchInsert(taskRecordsToInsert).execute();

			return tasksToInsert;
		});

		insertedTasks.forEach(taskCallbacks::callInsertHandlers);

		return insertedTasks;
	}

	public Optional<Task> startTask(Function<List<Task>, Optional<Task>> selector) {
		return databaseStorage.acquireWriteTransaction(db -> {
			List<Task> allTasks = db.selectFrom(TASK)
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

	public void deleteTasks(Collection<TaskId> tasks) {
		Set<String> taskIds = tasks.stream()
			.map(TaskId::getIdAsString)
			.collect(Collectors.toSet());

		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.deleteFrom(TASK)
				.where(TASK.ID.in(taskIds))
				.execute();
		}

		tasks.forEach(taskCallbacks::callDeleteHandlers);
	}

	public void deleteAllTasks() {
		Set<TaskId> taskIds;

		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			taskIds = db.selectFrom(TASK)
				.fetch(TASK.ID)
				.stream()
				.map(TaskId::fromString)
				.collect(Collectors.toSet());

			db.deleteFrom(TASK).execute();
		}

		taskIds.forEach(taskCallbacks::callDeleteHandlers);
	}

	public void setTaskPriority(TaskId taskId, TaskPriority newPriority) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.update(TASK)
				.set(TASK.PRIORITY, newPriority.asInt())
				.set(TASK.UPDATE_TIME, Instant.now())
				.where(TASK.ID.eq(taskId.getIdAsString()))
				.execute();
		}
	}

	public void setTaskStatus(TaskId taskId, boolean inProcess) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.update(TASK)
				.set(TASK.IN_PROCESS, inProcess)
				.set(TASK.UPDATE_TIME, Instant.now())
				.where(TASK.ID.eq(taskId.getIdAsString()))
				.execute();
		}
	}
}
