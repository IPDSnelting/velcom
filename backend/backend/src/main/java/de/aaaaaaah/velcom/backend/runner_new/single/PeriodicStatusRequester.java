package de.aaaaaaah.velcom.backend.runner_new.single;

import de.aaaaaaah.velcom.backend.access.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.access.entities.RunBuilder;
import de.aaaaaaah.velcom.backend.access.entities.Unit;
import de.aaaaaaah.velcom.backend.runner_new.single.state.AwaitClearResultReply;
import de.aaaaaaah.velcom.backend.runner_new.single.state.AwaitGetResultReply;
import de.aaaaaaah.velcom.backend.runner_new.single.state.AwaitGetStatusReply;
import de.aaaaaaah.velcom.backend.runner_new.single.state.TeleRunnerState;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Benchmark;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Metric;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClearResult;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.GetResult;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.GetStatus;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetResultReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetStatusReply;
import de.aaaaaaah.velcom.shared.protocol.statemachine.StateMachine;
import java.time.Instant;
import java.util.Optional;
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

				GetStatusReply statusReply = requestStatus();
				teleRunner.setRunnerInformation(statusReply);

				if (!statusReply.isResultAvailable()) {
					continue;
				}

				GetResultReply requestResults = requestResults();

				handleResults(requestResults);

				// TODO: Do sth with the result

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

	private void handleResults(GetResultReply requestResults) {
		boolean successful = requestResults.getResult()
			.flatMap(it -> Optional.ofNullable(it.getBenchmarks()))
			.isPresent();

		// TODO: 10.07.20 Memorize TASK and Start/End Time 
		if (successful) {
			Result result = requestResults.getResult().orElseThrow();
			RunBuilder builder = RunBuilder.successful(
				null,
				teleRunner.getRunnerName(),
				teleRunner.getRunnerInformation().getInformation(),
				Instant.EPOCH,
				Instant.EPOCH
			);

			//noinspection ConstantConditions
			for (Benchmark benchmark : result.getBenchmarks()) {
				for (Metric metric : benchmark.getMetrics()) {
					MeasurementName name = new MeasurementName(benchmark.getName(), metric.getName());
					if (metric.getError() != null) {
						builder.addFailedMeasurement(name, metric.getError());
					} else {
						//noinspection ConstantConditions
						builder.addSuccessfulMeasurement(
							name,
							Interpretation.fromSharedRepresentation(metric.getInterpretation()),
							new Unit(metric.getUnit()),
							metric.getValues()
						);
					}
				}
			}

			teleRunner.completeRun(builder.build());
		} else {
			RunBuilder builder = RunBuilder.failed(
				null,
				teleRunner.getRunnerName(),
				teleRunner.getRunnerInformation().getInformation(),
				Instant.EPOCH,
				Instant.EPOCH,
				requestResults.getError().orElseThrow()
			);
			teleRunner.completeRun(builder.build());
		}
	}

	/**
	 * Stops this requestor and tears down the thread. This object can not be reused.
	 */
	public void cancel() {
		cancelled = true;
		worker.interrupt();
	}
}
