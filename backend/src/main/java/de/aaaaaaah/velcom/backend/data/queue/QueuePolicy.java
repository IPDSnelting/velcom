package de.aaaaaaah.velcom.backend.data.queue;

import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.queue.Task;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.util.List;
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
	 */
	void addTask(Task task);

	/**
	 * A new task has been manually added to the queue.
	 *
	 * @param task the task that has been added
	 */
	void addManualTask(Task task);

	Optional<Task> getNextTask();

	/**
	 * @return all tasks in the order they are to be executed in. See {@link
	 *    Queue#viewAllCurrentTasks()} for more detail
	 */
	List<Task> viewAllCurrentTasks();

	/**
	 * A task has been aborted.
	 *
	 * @param repoId the repo the commit is in
	 * @param commitHash the commit the task is for
	 */
	void abortTask(RepoId repoId, CommitHash commitHash);
}
