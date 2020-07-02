package de.aaaaaaah.velcom.backend.data.queue;

import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * The queue policy decides the order in which the queue's tasks should be executed. A set of rules
 * that the policy must follow is detailed in the documentation of {@link Queue}.
 */
public interface QueuePolicy {

	/**
	 * A new task has been added to the queue.
	 *
	 * @param task the task that has been added
	 * @return true if the task was added, false if it was ignored because it is already contained
	 * 	in the policy
	 */
	boolean addTask(Task task);

	/**
	 * TODO: Docs
	 * @return
	 */
	Optional<Task> getNextTask();

	/**
	 * @return all tasks in the order they are to be executed in. See {@link
	 *    Queue#viewAllCurrentTasks()} for more detail
	 */
	List<Task> viewAllCurrentTasks();

	/**
	 * A task has been aborted.
	 *
	 * @param taskId the id of the task to abort
	 * @throws NoSuchElementException if not task with the given id is currently in the queue
	 */
	void abortTask(TaskId taskId) throws NoSuchElementException;

	/**
	 * Abort all tasks of a specific repo.
	 *
	 * @param repoId the repo whose tasks to abort
	 * @return the list of tasks that were aborted
	 */
	void abortAllTasksOfRepo(RepoId repoId);

	/**
	 * Aborts all tasks currently in the queue
	 * @return the list of tasks that were aborted
	 */
	void abortAllTasks();

}
