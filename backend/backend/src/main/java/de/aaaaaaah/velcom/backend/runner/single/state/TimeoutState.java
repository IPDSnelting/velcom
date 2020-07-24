package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.single.RunnerConnection;
import de.aaaaaaah.velcom.backend.runner.single.TeleRunner;
import de.aaaaaaah.velcom.shared.Timeout;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import java.time.Duration;

/**
 * A state that waits for a given timeout before closing the connection.
 */
public abstract class TimeoutState extends TeleRunnerState {

	private static final Duration TIMEOUT_DURATION = Duration.ofSeconds(10);
	private final Timeout timeout;

	public TimeoutState(TeleRunner runner, RunnerConnection connection) {
		super(runner, connection);

		timeout = Timeout.after(TIMEOUT_DURATION);
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
