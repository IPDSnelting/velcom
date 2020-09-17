package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.single.RunnerConnection;
import de.aaaaaaah.velcom.backend.runner.single.TeleRunner;

/**
 * Dummy state to avoid sending any new commands while the runner is receiving work.
 */
public class AwaitSendWorkEnd extends TeleRunnerState {

	private final TeleRunnerState before;
	private final Thread workerThread;

	public AwaitSendWorkEnd(TeleRunner runner, RunnerConnection connection, TeleRunnerState before) {
		super(runner, connection);
		this.before = before;

		this.workerThread = new Thread(() -> runner.sendAvailableWork(this));
	}

	@Override
	public void onEnter() {
		this.workerThread.start();
	}

	@Override
	public void onExit() {
		workerThread.interrupt();
	}

	public TeleRunnerState getBefore() {
		return before;
	}
}
