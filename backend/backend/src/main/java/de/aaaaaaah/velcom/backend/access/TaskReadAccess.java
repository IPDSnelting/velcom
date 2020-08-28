package de.aaaaaaah.velcom.backend.access;

import static org.jooq.codegen.db.Tables.TASK;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.RepoSource;
import de.aaaaaaah.velcom.backend.access.entities.TarSource;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.access.policy.QueuePolicy;
import de.aaaaaaah.velcom.backend.access.policy.RoundRobinFiloPolicy;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.codegen.db.tables.records.TaskRecord;

/**
 * Provides read access to task objects stored in a database.
 */
public class TaskReadAccess {

	protected final DatabaseStorage databaseStorage;
	protected final QueuePolicy policy;

	public TaskReadAccess(DatabaseStorage databaseStorage) {
		this.databaseStorage = Objects.requireNonNull(databaseStorage);
		this.policy = new RoundRobinFiloPolicy(databaseStorage);
	}

	public List<Task> getTasksSorted() {
		return policy.getTasksSorted();
	}

	/**
	 * @return the task that should be the next to be processed and benchmarked.
	 */
	public Optional<Task> fetchNextTask() {
		return policy.fetchNextTask();
	}

	/**
	 * Gets the task with the specified id.
	 *
	 * @param taskId the id of  the task
	 * @return the task
	 * @throws NoSuchTaskException if no task with the specified id exists
	 */
	public Task getById(TaskId taskId) throws NoSuchTaskException {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			TaskRecord taskRecord = db.fetchOne(TASK, TASK.ID.eq(taskId.getId().toString()));

			if (taskRecord == null) {
				throw new NoSuchTaskException(taskId);
			}

			if (taskRecord.getTarName() != null) {
				return new Task(
					taskId,
					taskRecord.getAuthor(),
					taskRecord.getPriority(),
					taskRecord.getInsertTime().toInstant(),
					taskRecord.getUpdateTime().toInstant(),
					new TarSource(taskRecord.getTarName())
				);
			} else {
				RepoId repoId = new RepoId(UUID.fromString(taskRecord.getRepoId()));
				CommitHash hash = new CommitHash(taskRecord.getCommitHash());

				return new Task(
					taskId,
					taskRecord.getAuthor(),
					taskRecord.getPriority(),
					taskRecord.getInsertTime().toInstant(),
					taskRecord.getUpdateTime().toInstant(),
					new RepoSource(repoId, hash)
				);
			}
		}
	}

	/**
	 * Constructs a new {@link Task} instance from a given task record.
	 *
	 * @param taskRecord the task record
	 * @return the task instance
	 */
	public static Task taskFromRecord(TaskRecord taskRecord) {
		if (taskRecord.getTarName() != null) {
			return new Task(
				new TaskId(UUID.fromString(taskRecord.getId())),
				taskRecord.getAuthor(),
				taskRecord.getPriority(),
				taskRecord.getInsertTime().toInstant(),
				taskRecord.getUpdateTime().toInstant(),
				new TarSource(taskRecord.getTarName())
			);
		} else {
			RepoId repoId = new RepoId(UUID.fromString(taskRecord.getRepoId()));
			CommitHash hash = new CommitHash(taskRecord.getCommitHash());

			return new Task(
				new TaskId(UUID.fromString(taskRecord.getId())),
				taskRecord.getAuthor(),
				taskRecord.getPriority(),
				taskRecord.getInsertTime().toInstant(),
				taskRecord.getUpdateTime().toInstant(),
				new RepoSource(repoId, hash)
			);
		}

	}

}
