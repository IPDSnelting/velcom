package de.aaaaaaah.velcom.backend.data.queue;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.ArchiveAccess;
import de.aaaaaaah.velcom.backend.access.BenchmarkWriteAccess;
import de.aaaaaaah.velcom.backend.access.TaskWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.entities.benchmark.NewRun;
import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.access.exceptions.PrepareTransferException;
import de.aaaaaaah.velcom.backend.access.exceptions.TransferException;
import de.aaaaaaah.velcom.backend.access.policy.QueuePriority;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The queue manages tasks that are passed to it from various sources and sorts these tasks to bring
 * them into an order in which they will be fetched for execution by the dispatcher. This class is
 * thread safe.
 *
 * <p>The order in which the tasks are sorted depends on the underlying {@link
 * de.aaaaaaah.velcom.backend.access.policy.QueuePolicy QueuePolicy} used in the access layer and is
 * not known to the queue itself.</p>
 *
 * <p>Initially upon construction, all tasks that are residing in the queue will have their
 * status manually set to "not in process".</p>
 */
public class Queue {

	private final TaskWriteAccess taskAccess;
	private final ArchiveAccess archiveAccess;
	private final BenchmarkWriteAccess benchAccess;

	private final Collection<Consumer<TaskId>> abortHandlers = new ArrayList<>();

	public Queue(TaskWriteAccess taskAccess, ArchiveAccess archiveAccess,
		BenchmarkWriteAccess benchAccess) {

		this.taskAccess = taskAccess;
		this.archiveAccess = archiveAccess;
		this.benchAccess = benchAccess;
	}

	/**
	 * Adds a handler to this queue that is called when a task process is aborted, which means that
	 * the task was given to a runner but the processing of the task has been subsequently cancelled.
	 * The task still remains in the queue.
	 *
	 * @param handler the handler
	 */
	public void onTaskProcessAbort(Consumer<TaskId> handler) {
		this.abortHandlers.add(handler);
	}

	/**
	 * Adds a handler to this queue that is called when a new task is inserted into the queue.
	 *
	 * @param handler the handler
	 */
	public void onTaskInsert(Consumer<Task> handler) {
		this.taskAccess.onTaskInsert(handler);
	}

	/**
	 * Adds a new handler to this queue which is called when a task is deleted from this queue.
	 *
	 * @param handler the handler
	 */
	public void onTaskDelete(Consumer<TaskId> handler) {
		this.taskAccess.onTaskDelete(handler);
	}

	/**
	 * Gets the task associated with the given task id.
	 *
	 * @param taskId the id of the task
	 * @return the task
	 * @throws NoSuchTaskException if no task with the given id exists
	 */
	public Task getTaskById(TaskId taskId) throws NoSuchTaskException {
		return this.taskAccess.getById(taskId);
	}

	/**
	 * Gets the task that is next in line to be benchmarked. The returned task will then be marked as
	 * "in_process" until the task is completed via {@link Queue#completeTask(NewRun)}.
	 *
	 * @return the next task in this queue to be processed
	 */
	public Optional<Task> fetchNextTask() {
		return taskAccess.fetchNextTask();
	}

	/**
	 * @return a list of all tasks that currently reside in the queue, sorted by their order of
	 * 	processing.
	 */
	public List<Task> getTasksSorted() {
		return taskAccess.getTasksSorted();
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
	public Collection<Task> addCommits(String author, RepoId repoId, List<CommitHash> hashes,
		QueuePriority priority) {

		List<Task> tasks = hashes.stream()
			.map(hash -> new Task(author, priority, new CommitSource(repoId, hash)))
			.collect(toList());

		return taskAccess.insertTasks(tasks);
	}

	/**
	 * Changes the priority of the task that matches the given task id.
	 *
	 * @param taskId the id of the task
	 * @param newPriority the new priority
	 */
	public void prioritizeTask(TaskId taskId, QueuePriority newPriority) {
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

		Task task = taskAccess.getById(taskId);
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
		abortHandlers.forEach(handler -> handler.accept(taskId));
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
	 * Deletes all tasks that are associated with the given repository.
	 *
	 * @param repoId the id of the repo
	 */
	public void deleteAllTasksOfRepo(RepoId repoId) {
		taskAccess.deleteAllTasksOfRepo(repoId);
	}

	/**
	 * Deletes all tasks from this queue.
	 */
	public void deleteAllTasks() {
		taskAccess.deleteAllTasks();
	}

}
