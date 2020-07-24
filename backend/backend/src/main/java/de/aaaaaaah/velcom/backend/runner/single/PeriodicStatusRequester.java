package de.aaaaaaah.velcom.backend.runner.single;

import de.aaaaaaah.velcom.backend.runner.single.state.AwaitClearResultReply;
import de.aaaaaaah.velcom.backend.runner.single.state.AwaitGetResultReply;
import de.aaaaaaah.velcom.backend.runner.single.state.AwaitGetStatusReply;
import de.aaaaaaah.velcom.backend.runner.single.state.TeleRunnerState;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClearResult;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.GetResult;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.GetStatus;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetResultReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetStatusReply;
import de.aaaaaaah.velcom.shared.protocol.statemachine.StateMachine;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * A thread that periodically asks a runner for its status.
 */
public class PeriodicStatusRequester {

	private final Thread worker;
	private final TeleRunner teleRunner;
	private final RunnerConnection connection;
	private final StateMachine<TeleRunnerState> stateMachine;
	private volatile boolean cancelled;

	public PeriodicStatusRequester(TeleRunner teleRunner, RunnerConnection connection,
		StateMachine<TeleRunnerState> stateMachine) {
		this.teleRunner = teleRunner;
		this.connection = connection;
		this.stateMachine = stateMachine;

		this.worker = new Thread(this::run, "PeriodicStatusRequester");
		this.worker.setDaemon(true);
	}

	/**
	 * Starts the requester.
	 */
	public void start() {
		this.worker.start();
	}

	private void run() {
		while (!cancelled) {
			try {
				//noinspection BusyWait
				Thread.sleep(5000);

				// TODO: 10.07.20 We can disconnect at any point. Handle it. 
				GetStatusReply statusReply = requestStatus();
				teleRunner.setRunnerInformation(statusReply);

				if (!statusReply.isResultAvailable()) {
					continue;
				}

				GetResultReply requestResults = requestResults();
				UUID runId = requestResults.getRunId();

				// The runner has a result - we don't know why :( Tell it to clear it and move on
				if (teleRunner.getCurrentTask().isEmpty()) {
					clearResults();
					continue;
				}
				// The runner has a *different* result than we expected. Disconnect.
				if (!teleRunner.getCurrentTask().get().getId().getId().equals(runId)) {
					clearResults();
					connection.close(StatusCode.ILLEGAL_BEHAVIOUR);
					continue;
				}

				teleRunner.handleResults(requestResults);

				clearResults();
			} catch (InterruptedException | ExecutionException | CancellationException ignored) {
			}
		}
	}

	private GetStatusReply requestStatus()
		throws InterruptedException, ExecutionException {
		AwaitGetStatusReply statusReplyState = new AwaitGetStatusReply(teleRunner, connection);

		stateMachine.switchFromRestingState(statusReplyState);
		connection.send(new GetStatus().asPacket(connection.getSerializer()));

		return statusReplyState.getReplyFuture().get();
	}

	private GetResultReply requestResults()
		throws InterruptedException, ExecutionException {
		AwaitGetResultReply resultReplyState = new AwaitGetResultReply(teleRunner, connection);
		stateMachine.switchFromRestingState(resultReplyState);
		connection.send(new GetResult().asPacket(connection.getSerializer()));

		return resultReplyState.getReplyFuture().get();
	}

	private void clearResults() throws InterruptedException, ExecutionException {
		AwaitClearResultReply clearResultState = new AwaitClearResultReply(teleRunner, connection);
		stateMachine.switchFromRestingState(clearResultState);
		connection.send(new ClearResult().asPacket(connection.getSerializer()));

		clearResultState.getReplyFuture().get();
	}

	/**
	 * Stops this requestor and tears down the thread. This object can not be reused.
	 */
	public void cancel() {
		cancelled = true;
		worker.interrupt();
	}
}
