package de.aaaaaaah.velcom.backend.runner;

import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import java.util.List;

/**
 * The dispatcher interface other modules can compile against.
 */
public interface IDispatcher {

	/**
	 * Aborts a given commit if it is currently being executed by a runner.
	 *
	 * @param runId the id of the task
	 * @return true if the commit was aborted, false if it wasn't being executed
	 */
	boolean abort(TaskId runId);

	/**
	 * @return a list with all known runners
	 */
	List<KnownRunner> getKnownRunners();
}
