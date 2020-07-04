package de.aaaaaaah.velcom.backend.runner_new.single.state;

import de.aaaaaaah.velcom.backend.runner_new.single.RunnerConnection;
import de.aaaaaaah.velcom.backend.runner_new.single.TeleRunner;
import de.aaaaaaah.velcom.runner.shared.Timeout;
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
			connection.close(5000, "Packet wait timed out!");
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
