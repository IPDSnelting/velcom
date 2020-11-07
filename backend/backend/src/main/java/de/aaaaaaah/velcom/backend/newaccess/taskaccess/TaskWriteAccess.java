package de.aaaaaaah.velcom.backend.newaccess.taskaccess;

import static org.jooq.codegen.db.tables.Task.TASK;

import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.shared.util.Either;
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
		Either<CommitSource, TarSource> source = task.getSource();
		Optional<RepoId> repoId = source
			.consume(it -> Optional.of(it.getRepoId()), TarSource::getRepoId);
		Optional<CommitHash> commitHash = source
			.consume(it -> Optional.of(it.getHash()), it -> Optional.empty());
		Optional<String> description = source
			.consume(it -> Optional.empty(), it -> Optional.of(it.getDescription()));

		TaskRecord record = new TaskRecord();
		record.setId(task.getIdAsString());
		record.setAuthor(task.getAuthor());
		record.setPriority(task.getPriority().asInt());
		record.setInsertTime(task.getInsertTime());
		record.setUpdateTime(task.getUpdateTime());
		record.setRepoId(repoId.map(RepoId::getIdAsString).orElse(null));
		record.setCommitHash(commitHash.map(CommitHash::getHash).orElse(null));
		record.setDescription(description.orElse(null));
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
		return databaseStorage.acquireWriteTransaction(db -> {
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
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			Set<String> taskIds = tasks.stream()
				.map(TaskId::getIdAsString)
				.collect(Collectors.toSet());

			db.deleteFrom(TASK)
				.where(TASK.ID.in(taskIds))
				.execute();
		}
	}

	public void deleteAllTasks() {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.deleteFrom(TASK).execute();
		}
	}

}
