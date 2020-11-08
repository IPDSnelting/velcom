package de.aaaaaaah.velcom.backend.newaccess.taskaccess;

import static org.jooq.codegen.db.tables.Task.TASK;
import static org.jooq.impl.DSL.not;

import de.aaaaaaah.velcom.backend.newaccess.AccessUtils;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.codegen.db.tables.records.TaskRecord;
import org.jooq.exception.DataAccessException;

public class TaskReadAccess {

	protected final DatabaseStorage databaseStorage;

	public TaskReadAccess(DatabaseStorage databaseStorage) {
		this.databaseStorage = databaseStorage;
	}

	protected static Task taskRecordToTask(TaskRecord record) {
		return new Task(
			TaskId.fromString(record.getId()),
			record.getAuthor(),
			TaskPriority.fromInt(record.getPriority()),
			record.getInsertTime(),
			record.getUpdateTime(),
			AccessUtils.readSource(record.getRepoId(), record.getCommitHash(), record.getDescription()),
			record.getInProcess()
		);
	}

	/**
	 * Gets the task with the specified id.
	 *
	 * @param taskId the id of the task
	 * @return the task
	 * @throws NoSuchTaskException if no task with the specified id exists
	 */
	public Task getTask(TaskId taskId) throws NoSuchTaskException {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			TaskRecord record = db.fetchSingle(TASK, TASK.ID.eq(taskId.getIdAsString()));
			return taskRecordToTask(record);
		} catch (DataAccessException e) {
			throw new NoSuchTaskException(e, taskId);
		}
	}

	/**
	 * A good way to check whether a task exists and to check its status if it exists.
	 *
	 * @param taskId the id of the task to check for
	 * @return the tasks status if the task exists, empty otherwise
	 */
	public Optional<Boolean> getTaskStatus(TaskId taskId) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			Boolean inProcess = db.selectFrom(TASK)
				.where(TASK.ID.eq(taskId.getIdAsString()))
				.fetchOne(TASK.IN_PROCESS);
			return Optional.ofNullable(inProcess);
		}
	}

	/**
	 * @return all tasks in no particular order
	 */
	public List<Task> getAllTasks() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectFrom(TASK)
				.stream()
				.map(TaskReadAccess::taskRecordToTask)
				.collect(Collectors.toList());
		}
	}

	/**
	 * @return all tasks that haven't been started yet (i. e. {@link Task#isInProcess()} returns
	 *  {@code false}) in no particular order
	 */
	public List<Task> getAllUnstartedTasks() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectFrom(TASK)
				.where(not(TASK.IN_PROCESS))
				.stream()
				.map(TaskReadAccess::taskRecordToTask)
				.collect(Collectors.toList());
		}
	}
}
