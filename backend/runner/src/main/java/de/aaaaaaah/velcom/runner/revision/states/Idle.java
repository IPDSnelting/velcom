package de.aaaaaaah.velcom.runner.revision.states;

import de.aaaaaaah.velcom.runner.revision.TeleBackend;
import de.aaaaaaah.velcom.runner.revision.Connection;

/**
 * This is the (only) resting state in the runner state machine. It has no special behaviour
 * otherwise.
 */
public class Idle extends RunnerState {

	public Idle(TeleBackend teleBackend, Connection connection) {
		super(teleBackend, connection);
	}

	@Override
	public boolean isResting() {
		return true;
	}
}
