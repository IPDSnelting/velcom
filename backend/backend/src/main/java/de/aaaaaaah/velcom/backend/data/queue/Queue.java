package de.aaaaaaah.velcom.backend.data.queue;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.ArchiveAccess;
import de.aaaaaaah.velcom.backend.access.BenchmarkWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.benchmark.NewRun;
import de.aaaaaaah.velcom.backend.access.exceptions.PrepareTransferException;
import de.aaaaaaah.velcom.backend.access.exceptions.TransferException;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.TaskWriteAccess;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.shared.util.Either;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The queue manages tasks that are passed to it from various sources and sorts these tasks to bring
 * them into an order in which they will be fetched for execution by the dispatcher. This class is
 * thread safe.
 *
 * <p> The order in which the tasks are sorted is determined by the queue's policy.
 */
public class Queue {

	private final TaskWriteAccess taskAccess;
	private final ArchiveAccess archiveAccess;
	private final BenchmarkWriteAccess benchAccess;

	private final AtomicReference<Optional<RepoId>> currentRepoId;

	public Queue(TaskWriteAccess taskAccess, ArchiveAccess archiveAccess,
		BenchmarkWriteAccess benchAccess) {

		this.taskAccess = taskAccess;
		this.archiveAccess = archiveAccess;
		this.benchAccess = benchAccess;

		currentRepoId = new AtomicReference<>(Optional.empty());
	}

	/**
	 * Gets the task associated with the given task id.
	 *
	 * @param taskId the id of the task
	 * @return the task
	 * @throws NoSuchTaskException if no task with the given id exists
	 */
	public Task getTaskById(TaskId taskId) throws NoSuchTaskException {
		return taskAccess.getTask(taskId);
	}

	/**
	 * Gets the task that is next in line to be benchmarked. The returned task will then be marked as
	 * "in_process" until the task is completed via {@link #completeTask(NewRun)}.
	 *
	 * @return the next task in this queue to be processed
	 */
	public Optional<Task> fetchNextTask() {
		return taskAccess.startTask(tasks -> {
			Policy policy = new Policy(tasks, currentRepoId.get().orElse(null));
			Optional<Task> nextTask = policy.step();

			if (nextTask.isPresent()) {
				currentRepoId.set(policy.getCurrentRepoId());
			}

			return nextTask;
		});
	}

	/**
	 * @return a list of all tasks that currently reside in the queue, sorted by their order of
	 * 	processing.
	 */
	public List<Task> getTasksSorted() {
		List<Task> allTasks = taskAccess.getAllUnstartedTasks();
		Policy policy = new Policy(allTasks, currentRepoId.get().orElse(null));
		return policy.stepAll();
	}

	/**
	 * Adds the specified commits as tasks into the queue.
	 *
	 * @param author the author of this addition
	 * @param repoId the id of the repository that the given commits belong to
	 * @param hashes the commit hashes
	 * @param priority the priority that will be given to the commit tasks
	 * @return a collection of all tasks that were actually inserted because respective their commits
	 * 	were not already in the queue beforehand
	 */
	// TODO: 07.11.20 Mention how the order of the commits is preserved
	public Collection<Task> addCommits(String author, RepoId repoId, List<CommitHash> hashes,
		TaskPriority priority) {

		List<Task> tasks = hashes.stream()
			.map(hash -> new Task(author, priority, Either.ofLeft(new CommitSource(repoId, hash))))
			.collect(toList());

		return taskAccess.insertOrIgnoreTasks(tasks);
	}

	/**
	 * Changes the priority of the task that matches the given task id.
	 *
	 * @param taskId the id of the task
	 * @param newPriority the new priority
	 */
	public void prioritizeTask(TaskId taskId, TaskPriority newPriority) {
		taskAccess.setTaskPriority(taskId, newPriority);
	}

	/**
	 * Transfers a task to the supplied {@link OutputStream}.
	 *
	 * <p>Note that the provided output stream will be closed after the transfer operation is
	 * done.</p>
	 *
	 * @param taskId the id of the task to be transferred
	 * @param output the output to transfer the task to
	 * @throws NoSuchTaskException if no task with the given id exists
	 * @throws TransferException if the transfer process itself failed
	 * @throws PrepareTransferException if the preparation to transfer failed
	 */
	public void transferTask(TaskId taskId, OutputStream output)
		throws NoSuchTaskException, TransferException, PrepareTransferException {

		Task task = taskAccess.getTask(taskId);
		archiveAccess.transferTask(task, output);
	}

	/**
	 * Cancels the processing of the task with the given task id. This merely marks the task as "not
	 * in process" anymore and will not delete the task from the queue.
	 *
	 * @param taskId the id of the task
	 */
	public void abortTaskProcess(TaskId taskId) {
		taskAccess.setTaskStatus(taskId, false);
	}

	/**
	 * Marks the task associated with the given run as complete which means that the task will be
	 * deleted from the queue.
	 *
	 * @param result the result associated with the task
	 */
	public void completeTask(NewRun result) {
		benchAccess.insertRun(result);
	}

	/**
	 * Deletes the tasks that are associated with the given task ids from this queue.
	 *
	 * @param taskIds the ids of the tasks
	 */
	public void deleteTasks(Collection<TaskId> taskIds) {
		taskAccess.deleteTasks(taskIds);
	}

	/**
	 * Deletes all tasks from this queue.
	 */
	public void deleteAllTasks() {
		taskAccess.deleteAllTasks();
	}

}
