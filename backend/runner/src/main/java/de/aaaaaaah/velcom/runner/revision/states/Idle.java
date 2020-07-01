package de.aaaaaaah.velcom.runner.revision.states;

import de.aaaaaaah.velcom.runner.revision.Backend;
import de.aaaaaaah.velcom.runner.revision.Connection;

/**
 * This is the (only) resting state in the runner state machine. It has no special behaviour
 * otherwise.
 */
public class Idle extends RunnerState {

	public Idle(Backend backend, Connection connection) {
		super(backend, connection);
	}

	@Override
	public boolean isResting() {
		return true;
	}
}
