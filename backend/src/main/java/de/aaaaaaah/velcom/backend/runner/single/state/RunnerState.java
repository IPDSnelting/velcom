package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;

/**
 * A state the server's runner state machine can be in.
 */
public interface RunnerState {

	/**
	 * Called when this state is selected.
	 *
	 * @param information information about the runner
	 * @apiNote The default implementation does nothing
	 */
	default void onSelected(ActiveRunnerInformation information) {
	}

	/**
	 * Called when the runner sent a message.
	 *
	 * @param type the type of the message
	 * @param entity the deserialized message entity
	 * @param information information about the runner
	 * @return the new state. If it is "this", onSelected will not be called
	 * @apiNote The default implementation returns this
	 */
	default RunnerState onMessage(String type, SentEntity entity,
		ActiveRunnerInformation information) {
		return this;
	}
}
