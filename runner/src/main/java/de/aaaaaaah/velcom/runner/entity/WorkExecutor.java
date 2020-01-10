package de.aaaaaaah.velcom.runner.entity;

import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import java.nio.file.Path;

/**
 * Starts execution of whatever work the server sent.
 */
public interface WorkExecutor {

	/**
	 * Aborts execution and discards all (potential) results.
	 */
	void abortExecution();

	/**
	 * Starts execution.
	 *
	 * @param workPath the path to the work (probably a tar)
	 * @param workOrder the work order
	 * @param configuration the runner configuration
	 */
	void startExecution(Path workPath, RunnerWorkOrder workOrder,
		RunnerConfiguration configuration);
}
