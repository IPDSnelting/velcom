package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.Delays;
import de.aaaaaaah.velcom.backend.runner.single.RunnerConnection;
import de.aaaaaaah.velcom.backend.runner.single.TeleRunner;
import de.aaaaaaah.velcom.shared.util.Timeout;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;

/**
 * A state that waits for a given timeout before closing the connection.
 */
public abstract class TimeoutState extends TeleRunnerState {

	private final Timeout timeout;

	public TimeoutState(TeleRunner runner, RunnerConnection connection) {
		super(runner, connection);

		timeout = Timeout.after(Delays.AWAIT_COMMAND_REPLY);
		timeout.getCompletionStage().thenRun(() -> {
			connection.close(StatusCode.COMMAND_TIMEOUT);
		});
	}

	@Override
	public void onEnter() {
		timeout.start();
	}

	@Override
	public void onExit() {
		timeout.cancel();
	}
}
