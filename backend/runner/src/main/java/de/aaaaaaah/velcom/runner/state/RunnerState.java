package de.aaaaaaah.velcom.runner.state;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A single state the runner can be in.
 */
public interface RunnerState {

	/**
	 * @return the status of the runner if it is in that state
	 */
	RunnerStatusEnum getStatus();

	/**
	 * Called when this stage is selected.
	 *
	 * @param configuration the runner configuration
	 * @throws IOException if an error occurs
	 */
	@SuppressWarnings("RedundantThrows")
	default void onSelected(RunnerConfiguration configuration) throws IOException {
	}

	/**
	 * Called with the saved path when a file was received.
	 *
	 * @param path the path to the received file. Most likely a temporary directory and deleting it
	 * 	is explicitly allowed.
	 * @param configuration the runner configuration
	 * @return the new state. If this returns this, onSelected will	not be called
	 * @throws IOException if an error occurs
	 * @implNote The default implementation returns this.
	 */
	default RunnerState onFileReceived(Path path, RunnerConfiguration configuration)
		throws IOException {
		return this;
	}

	/**
	 * Called when the runner receives some work order from the server.
	 *
	 * @param workOrder the work to complete
	 * @param configuration the runner configuration
	 * @return the new state. If this returns this, onSelected will	not be called
	 * @throws IOException if an error occurs
	 * @implNote The default implementation returns this.
	 */
	default RunnerState onWorkArrived(RunnerWorkOrder workOrder, RunnerConfiguration configuration)
		throws IOException {
		return this;
	}
}
