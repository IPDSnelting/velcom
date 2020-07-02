package de.aaaaaaah.velcom.backend.data.queue;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import de.aaaaaaah.velcom.backend.ServerMain;
import de.aaaaaaah.velcom.backend.access.KnownCommitWriteAccess;
import de.aaaaaaah.velcom.backend.access.TaskWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.*;

import java.util.*;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * The queue is passed tasks from various sources. It keeps track of all tasks and updates their
 * benchmark status in the {@link KnownCommitWriteAccess}, and also passes all tasks to an internal
 * {@link QueuePolicy}. That policy keeps the tasks and decides in which order they should be
 * executed.
 *
 * <p> When the queue is loaded after a restart, all the known commits which still need to be
 * benchmarked as well as the manual commits need to be added one by one again. The queue does
 * <em>>not</em> do this itself.
 *
 * <p> All publicly available functions in the queue are synchronized, which should make the queue
 * threadsafe to use.
 *
 * <p> The policy must follow these rules:
 * <ul>
 *     <li>There is always at most one task per commit.</li>
 *     <li>The policy must never drop or ignore a commit.</li>
 *     <li>If a manual task is added for a commit, that always replaces a previous non-manual task.</li>
 *     <li>A non-manual task never replaces a manual task.</li>
 * </ul>
 * 
 * TODO: Completely rewrite docs for queue
 */
public class Queue {

	private static final Logger LOGGER = LoggerFactory.getLogger(Queue.class);

	private final TaskWriteAccess taskAccess;
	private final QueuePolicy queuePolicy;

	private final Collection<Consumer<Task>> somethingAddedListeners = new ArrayList<>();
	private final Collection<Consumer<TaskId>> somethingAbortedListeners = new ArrayList<>();

	public Queue(TaskWriteAccess taskAccess, QueuePolicy queuePolicy) {
		this.taskAccess = taskAccess;
		this.queuePolicy = queuePolicy;

		this.taskAccess.onTaskInsert(task -> somethingAddedListeners.forEach(l -> l.accept(task)));
		this.taskAccess.onTaskDelete(
			task -> somethingAbortedListeners.forEach(l -> l.accept(task)));

		ServerMain.getMetricRegistry().register(
			MetricRegistry.name(getClass(), "queue_length"),
			(Gauge<Integer>) () -> viewAllCurrentTasks().size()
		);
	}

	public synchronized Optional<Task> getNextTask() {
		final Optional<Task> nextTask = queuePolicy.getNextTask();
		nextTask.ifPresent(commit -> LOGGER.info("Task " + commit + " has been started"));
		return nextTask;
	}

	/**
	 * Get all tasks currently in the queue, in the order they are going to be executed in. In other
	 * words, repeated calls to {@link #getNextTask()} will result in this order of tasks, unless
	 * new tasks are added or tasks removed during that time.
	 *
	 * @return the list of tasks in the order they will be executed
	 */
	public synchronized List<Task> viewAllCurrentTasks() {
		return queuePolicy.viewAllCurrentTasks();
	}

	public synchronized void prioritizeTask(TaskId taskId, int newPriority) {
		taskAccess.setTaskPriority(taskId, newPriority);
	}

	/**
	 * Add a new non-manual task to the queue. This ignores and overwrites any existing {@link
	 * BenchmarkStatus}.
	 *
	 * @param task the commit that should be added as task
	 */
	public synchronized void addTask(Task task) {
		if (queuePolicy.addTask(task)) {
			taskAccess.insertTasks(List.of(task));
			LOGGER.info("Added task " + task + " to queue and notified all listeners");
		} else {
			LOGGER.debug("Didn't add task " + task + " as it was already added earlier");
		}
	}

	/**
	 * Abort a task.
	 *
	 * @param taskId the id of the task to abort
	 * @throws NoSuchElementException if no task with the given id is currently in this queue
	 */
	public synchronized void abortTask(TaskId taskId) throws NoSuchElementException {
		queuePolicy.abortTask(taskId);
		taskAccess.deleteTasks(List.of(taskId));
		LOGGER.info(taskId + " was aborted");
	}

	/**
	 * Remove all tasks of a repo from the queue.
	 *
	 * @param repoId the id of the repo
	 */
	public synchronized void abortAllTasksOfRepo(RepoId repoId) {
		queuePolicy.abortAllTasksOfRepo(repoId);
		taskAccess.deleteAllTasksOfRepo(repoId);
		LOGGER.info("All tasks with repoId " + repoId + " were aborted");
	}

	/**
	 * Removes all tasks from this queue.
	 */
	public synchronized void abortAllTasks() {
		queuePolicy.abortAllTasks();
		taskAccess.deleteAllTasks();
		LOGGER.info("All tasks were aborted");
	}

	/**
	 * Registers a listener that is called when something is added to the queue.
	 *
	 * @param listener the listener to call if something is added to the queue
	 */
	public synchronized void onTaskAdded(Consumer<Task> listener) {
		somethingAddedListeners.add(listener);
	}

	/**
	 * Registers a listener that is called when a task is aborted.
	 *
	 * @param listener the listener to call if a task is aborted
	 */
	public synchronized void onTaskAborted(Consumer<TaskId> listener) {
		somethingAbortedListeners.add(listener);
	}

}
