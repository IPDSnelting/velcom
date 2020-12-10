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
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thread that periodically asks a runner for its status.
 */
public class PeriodicStatusRequester {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicStatusRequester.class);

	private final Thread worker;
	private final TeleRunner teleRunner;
	private final RunnerConnection connection;
	private final StateMachine<TeleRunnerState> stateMachine;
	private volatile boolean cancelled;
	private final Duration sleepBetweenIteration;

	public PeriodicStatusRequester(TeleRunner teleRunner, RunnerConnection connection,
		StateMachine<TeleRunnerState> stateMachine) {
		this(teleRunner, connection, stateMachine, Duration.ofSeconds(5));
	}

	public PeriodicStatusRequester(TeleRunner teleRunner, RunnerConnection connection,
		StateMachine<TeleRunnerState> stateMachine, Duration sleepBetweenIteration) {
		this.teleRunner = teleRunner;
		this.connection = connection;
		this.stateMachine = stateMachine;
		this.sleepBetweenIteration = sleepBetweenIteration;

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
				iteration();
				//noinspection BusyWait
				Thread.sleep(sleepBetweenIteration.toMillis());
			} catch (Exception e) {
				LOGGER.error("Error communicating with runner or handling results", e);
				try {
					clearResults();
				} catch (Exception e2) {
					LOGGER.error("Error telling runner to clear results", e2);
					connection.close(StatusCode.INTERNAL_ERROR);
				}
			}
		}
	}

	private void iteration() throws ExecutionException {
		try {
			GetStatusReply statusReply = requestStatus();
			teleRunner.setRunnerInformation(statusReply);

			if (!statusReply.isResultAvailable()) {
				return;
			}

			GetResultReply requestResults = requestResults();
			UUID runId = requestResults.getRunId();

			// The runner has a result - we don't know why :( Tell it to clear it and move on
			if (teleRunner.getCurrentTask().isEmpty()) {
				LOGGER.info(
					"{} had a result but we don't know why. Clearing it.", teleRunner.getRunnerName()
				);
				clearResults();
				return;
			}
			// The runner has a *different* result than we expected. Disconnect.
			if (!teleRunner.getCurrentTask().get().getId().getId().equals(runId)) {
				LOGGER.info(
					"{} had a different result than we expected: {} instead of {}.",
					teleRunner.getRunnerName(), runId, teleRunner.getCurrentTask().get().getId().getId()
				);
				clearResults();
				connection.close(StatusCode.ILLEGAL_BEHAVIOUR);
				return;
			}

			LOGGER.info("Got results for run {} from {}", runId, teleRunner.getRunnerName());
			teleRunner.handleResults(requestResults);

			clearResults();
		} catch (InterruptedException | CancellationException ignored) {
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
