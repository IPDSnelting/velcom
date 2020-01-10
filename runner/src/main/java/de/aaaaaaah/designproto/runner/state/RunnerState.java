package de.aaaaaaah.designproto.runner.state;

import de.aaaaaaah.designproto.runner.entity.RunnerConfiguration;
import de.aaaaaaah.designproto.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A single state the runner can be in.
 */
public interface RunnerState {

	/**
	 * Called when this stage is selected.
	 *
	 * @param configuration the runner configuration
	 * @return the new state. If this returns this, onSelected will	not be called
	 * @throws IOException if an error occurs
	 * @implNote The default implementation returns this.
	 */
	default RunnerState onSelected(RunnerConfiguration configuration) throws IOException {
		return this;
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

	/**
	 * The different states the runner can be in.
	 */
	enum State {
		/**
		 * The runner is initializing itself and or the connection to the remote server.
		 */
		INITIALIZING,
		/**
		 * The runner is connected and ready to serve requests.
		 */
		IDLE,
		/**
		 * The runner is waiting for a work binary.
		 */
		WAITING_FOR_WORK_BINARY,
		/**
		 * The runner is executing the sent work.
		 */
		EXECUTING
	}
}
