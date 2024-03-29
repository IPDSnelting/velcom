package de.aaaaaaah.velcom.backend.access.taskaccess;

import static org.jooq.codegen.db.tables.Task.TASK;

import de.aaaaaaah.velcom.backend.access.AccessUtils;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.access.taskaccess.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.tar.TarFileStorage;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.codegen.db.tables.records.TaskRecord;
import org.jooq.exception.DataAccessException;

/**
 * Access for retrieving tasks and their status.
 */
public class TaskReadAccess {

	protected final DatabaseStorage databaseStorage;
	protected final TarFileStorage tarFileStorage;

	public TaskReadAccess(DatabaseStorage databaseStorage, TarFileStorage tarFileStorage) {
		this.databaseStorage = databaseStorage;
		this.tarFileStorage = tarFileStorage;
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
			TaskRecord record = db.dsl().fetchSingle(TASK, TASK.ID.eq(taskId.getIdAsString()));
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
	public Optional<Boolean> isTaskInProgress(TaskId taskId) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			Boolean inProcess = db.dsl()
				.selectFrom(TASK)
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
			return db.dsl()
				.selectFrom(TASK)
				.stream()
				.map(TaskReadAccess::taskRecordToTask)
				.collect(Collectors.toList());
		}
	}
}
