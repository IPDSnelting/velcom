package de.aaaaaaah.velcom.backend.runner_new.single.state;

import de.aaaaaaah.velcom.backend.runner_new.single.RunnerConnection;
import de.aaaaaaah.velcom.backend.runner_new.single.TeleRunner;

/**
 * The tele-runner is in an idle state.
 */
public class IdleState extends TeleRunnerState {

	public IdleState(TeleRunner runner, RunnerConnection connection) {
		super(runner, connection);
	}

	@Override
	public boolean isResting() {
		return true;
	}
}
