package de.aaaaaaah.velcom.backend.newaccess.taskaccess;

import static org.jooq.codegen.db.tables.Task.TASK;
import static org.jooq.impl.DSL.not;

import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.tar.TarFileStorage;
import de.aaaaaaah.velcom.shared.util.Either;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jooq.codegen.db.tables.records.TaskRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskWriteAccess extends TaskReadAccess {

	private static final Logger LOGGER = LoggerFactory.getLogger(TaskWriteAccess.class);

	public TaskWriteAccess(DatabaseStorage databaseStorage, TarFileStorage tarFileStorage) {
		super(databaseStorage, tarFileStorage);
	}

	private static TaskRecord taskToTaskRecord(Task task) {
		return new TaskRecord(
			task.getIdAsString(),
			task.getAuthor(),
			task.getPriority().asInt(),
			task.getInsertTime(),
			task.getUpdateTime(),
			task.getRepoId().map(RepoId::getIdAsString).orElse(null),
			task.getCommitHash().map(CommitHash::getHash).orElse(null),
			task.getTarDescription().orElse(null),
			task.isInProgress()
		);
	}

	/**
	 * Insert a single commit if no corresponding task already exists and return the newly created
	 * task.
	 *
	 * @param author the new task's author
	 * @param priority the new task's priority
	 * @param repoId the commit's repo id
	 * @param hash the commit's hash
	 * @return the newly created task if no task corresponding to this commit already existed
	 */
	public Optional<Task> insertCommit(String author, TaskPriority priority, RepoId repoId,
		CommitHash hash) {

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
	 * @param priority the new task's priority
	 * @param repoId the commit's repo id
	 * @param hashes the commit hashes
	 */
	public void insertCommits(String author, TaskPriority priority, RepoId repoId,
		Collection<CommitHash> hashes) {

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
	 * Insert a tar file as a new task.
	 *
	 * @param author the new task's author
	 * @param priority the new task's priority
	 * @param description the tar file's description
	 * @param repoId the associated repo id, if any
	 * @param inputStream the tar file contents
	 * @return the newly created task (if a task was created)
	 */
	public Optional<Task> insertTar(String author, TaskPriority priority, String description,
		@Nullable RepoId repoId, InputStream inputStream) {

		TarSource source = new TarSource(description, repoId);
		Task task = new Task(author, priority, Either.ofRight(source));

		try {
			tarFileStorage.storeTarFile(task.getIdAsString(), inputStream);
		} catch (IOException ignore) {
			LOGGER.warn("Failed to store tar file for {}, task was not created", task);
			return Optional.empty(); // We can't store tar files for some reason?
		}

		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			TaskRecord record = taskToTaskRecord(task);
			// I think this shouldn't throw an exception if it couldn't insert the task and just silently
			// fail (failure signalled via return value), but I'm not entirely sure.
			db.dsl().batchInsert(record).execute();
		}

		return Optional.of(task);
	}

	/**
	 * Atomically select a task to start and start it via a custom selector function.
	 *
	 * @param selector The selector is passed a list of all tasks with status "not in progress". The
	 * 	list is in no particular order. If the selector returns a task, that task will be started and
	 * 	an updated version of it returned. The task returned by the selector <em>must be one of the
	 * 	input tasks</em>!
	 * @return the task chosen by the selector, if it returned one
	 */
	public Optional<Task> startTask(Function<List<Task>, Optional<TaskId>> selector) {
		return databaseStorage.acquireWriteTransaction(db -> {
			List<Task> allTasks = db.selectFrom(TASK)
				.where(not(TASK.IN_PROCESS))
				.stream()
				.map(TaskReadAccess::taskRecordToTask)
				.collect(Collectors.toList());

			Set<TaskId> allTaskIds = allTasks.stream()
				.map(Task::getId)
				.collect(Collectors.toSet());

			Optional<TaskId> taskToStart = selector.apply(allTasks);

			if (taskToStart.isEmpty()) {
				return Optional.empty();
			} else if (!allTaskIds.contains(taskToStart.get())) {
				// The selector has returned an invalid task, which we'll just ignore.
				// TODO: 08.11.20 Be more aggressive, throw an exception?
				return Optional.empty();
			}

			// The selector has returned a valid task

			String taskIdAsString = taskToStart.get().getIdAsString();
			db.update(TASK)
				.set(TASK.IN_PROCESS, true)
				.where(TASK.ID.eq(taskIdAsString))
				.execute();

			TaskRecord record = db.selectFrom(TASK)
				.where(TASK.ID.eq(taskIdAsString))
				.fetchOne();
			return Optional.of(taskRecordToTask(record));
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

		for (TaskId task : tasks) {
			try {
				tarFileStorage.removeTarFile(task.getIdAsString());
			} catch (IOException ignore) {
				LOGGER.warn("Failed to remove tar file for {}", task);
				// Not too bad since it will be cleaned up sooner or later anyways.
			}
		}
	}

	/**
	 * Delete all tasks.
	 */
	public void deleteAllTasks() {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.deleteFrom(TASK).execute();
		}

		try {
			tarFileStorage.removeAllFiles();
		} catch (IOException ignore) {
			LOGGER.warn("Failed to remove all tar files");
			// Not too bad since they will be cleaned up sooner or later anyways.
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

	/**
	 * Remove all tar files that don't belong to a tar currently in the queue.
	 *
	 * @throws IOException if something went wrong while deleting the tar files
	 */
	public void cleanUpTarFiles() throws IOException {
		Set<String> tarTaskIds;
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			tarTaskIds = db.selectFrom(TASK)
				.where(TASK.COMMIT_HASH.isNull())
				.stream()
				.map(TaskRecord::getId)
				.collect(Collectors.toSet());
		}

		tarFileStorage.removeUnknownFiles(tarTaskIds);
	}
}
