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
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.codegen.db.tables.records.TaskRecord;

public class TaskReadAccess {

	protected final DatabaseStorage databaseStorage;
	protected QueuePolicy policy;

	public TaskReadAccess(DatabaseStorage databaseStorage) {
		this.databaseStorage = Objects.requireNonNull(databaseStorage);
	}

	public List<Task> getTasksSorted() {
		return policy.getTasksSorted();
	}

	public Optional<Task> fetchNextTask() {
		return policy.fetchNextTask();
	}

	public Task getById(TaskId taskId) throws NoSuchTaskException {
		try (DSLContext db = databaseStorage.acquireContext()) {
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
