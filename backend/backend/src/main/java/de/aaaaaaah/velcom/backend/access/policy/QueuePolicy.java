package de.aaaaaaah.velcom.backend.access.policy;

import de.aaaaaaah.velcom.backend.access.entities.Task;
import java.util.List;
import java.util.Optional;

/**
 * Determines how tasks are ordered by providing a sorted view of all tasks and providing a method
 * of just getting the next task in line.
 */
public interface QueuePolicy {

	/**
	 * Gets the task that is next in line to be benchmarked. The returned task will then be marked as
	 * "in_process" until the task is completed.
	 *
	 * <p>Implementations of this method must guarantee thread safety.</p>
	 *
	 * @return the next task to be benchmarked.
	 */
	Optional<Task> fetchNextTask();

	/**
	 * @return a sorted view of all tasks.
	 */
	List<Task> getTasksSorted();

}
