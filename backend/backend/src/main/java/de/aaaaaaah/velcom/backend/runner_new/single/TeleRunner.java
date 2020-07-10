package de.aaaaaaah.velcom.backend.runner_new.single;

import de.aaaaaaah.velcom.backend.access.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunBuilder;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.entities.Unit;
import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.access.exceptions.PrepareTransferException;
import de.aaaaaaah.velcom.backend.access.exceptions.TransferException;
import de.aaaaaaah.velcom.backend.runner_new.Dispatcher;
import de.aaaaaaah.velcom.backend.runner_new.KnownRunner;
import de.aaaaaaah.velcom.backend.runner_new.single.state.AwaitAbortRunReply;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.Converter;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Benchmark;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Metric;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.AbortRun;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.RequestRunReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetResultReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetStatusReply;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server side class for a runner.
 */
public class TeleRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(TeleRunner.class);

	private final AtomicReference<GetStatusReply> runnerInformation;
	private final String runnerName;
	private final Converter serializer;
	private final Dispatcher dispatcher;
	private final AtomicReference<Task> myCurrentTask;

	private RunnerConnection connection;

	public TeleRunner(String runnerName, Converter serializer, Dispatcher dispatcher) {
		this.runnerName = runnerName;
		this.serializer = serializer;
		this.dispatcher = dispatcher;
		this.runnerInformation = new AtomicReference<>();
		this.myCurrentTask = new AtomicReference<>();

		this.connection = createConnection();
	}

	/**
	 * Creates a new connection, if none exists.
	 *
	 * @return the created connection
	 * @throws IllegalStateException if this runner already has a connection
	 */
	public synchronized RunnerConnection createConnection() throws IllegalStateException {
		if (connection != null) {
			throw new IllegalStateException("I already have a connection");
		}
		connection = new RunnerConnection(serializer, this);
		connection.addCloseListener(this::disposeConnection);

		return connection;
	}

	private void disposeConnection() {
		synchronized (this) {
			this.connection = null;
		}
	}

	/**
	 * @return true if this runner has an active connection.
	 */
	public synchronized boolean hasConnection() {
		return connection != null;
	}

	/**
	 * Returns the unique name for this runner.
	 *
	 * @return the runner name
	 */
	public String getRunnerName() {
		return runnerName;
	}

	/**
	 * Sets the runner information.
	 *
	 * @param reply the reply
	 */
	public void setRunnerInformation(GetStatusReply reply) {
		runnerInformation.set(reply);
	}

	/**
	 * @return the current task of this runner, if any
	 */
	public Optional<Task> getCurrentTask() {
		return Optional.ofNullable(myCurrentTask.get());
	}

	/**
	 * Returns the runner information. This must always be non-null from the moment the runner is
	 * registered to the dispatcher. The value might be outdated.
	 */
	public KnownRunner getRunnerInformation() {
		GetStatusReply reply = runnerInformation.get();

		return new KnownRunner(
			getRunnerName(), reply.getInfo(), reply.getStatus(), myCurrentTask.get()
		);
	}

	/**
	 * Aborts the current benchmark. This is a very rudimentary implementation at the moment.
	 * <br>
	 * The abort will do nothing, if
	 * <ul>
	 *   <li>The runner is not connected</li>
	 *   <li>The send fails</li>
	 *   <li>The runner reconnects and has results for that run</li>
	 *   <li>The server sends a get_status before the abort and the runner has results</li>
	 * </ul>
	 * These limitations might be lifted in the future.
	 */
	public void abort() {
		myCurrentTask.set(null);

		if (!hasConnection()) {
			LOGGER.info("Tried to abort commit but was not connected with a runner: {}", getRunnerName());
			return;
		}

		try {
			connection.getStateMachine().switchFromRestingState(new AwaitAbortRunReply(this, connection));
			connection.send(new AbortRun().asPacket(serializer));
		} catch (InterruptedException e) {
			LOGGER.warn("Abort failed, I was interrupted {}", getRunnerName());
		}
	}

	/**
	 * Handles a result, saving the data in the DB if the task matches the stored one.
	 *
	 * @param resultReply the results
	 */
	public void handleResults(GetResultReply resultReply) {
		Task task = myCurrentTask.get();
		if (task == null) {
			// Somehow we have no task but a result, retry that commit. This should not happen, but if it
			// does err on the side of caution. Retry that task if possible, better to benchmark twice
			// than never
			dispatcher.getQueue().abortTaskProcess(new TaskId(resultReply.getRunId()));
			return;
		}

		boolean successful = resultReply.getResult()
			.flatMap(it -> Optional.ofNullable(it.getBenchmarks()))
			.isPresent();

		// TODO: 10.07.20 Memorize Start/End Time
		if (successful) {
			Result result = resultReply.getResult().orElseThrow();
			RunBuilder builder = RunBuilder.successful(
				task,
				getRunnerName(),
				getRunnerInformation().getInformation(),
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

			dispatcher.completeTask(builder.build());
		} else {
			RunBuilder builder = RunBuilder.failed(
				task,
				getRunnerName(),
				getRunnerInformation().getInformation(),
				Instant.EPOCH,
				Instant.EPOCH,
				resultReply.getError().orElseThrow()
			);
			dispatcher.completeTask(builder.build());
		}

		myCurrentTask.set(null);
	}

	/**
	 * Sends a {@link RequestRunReply} and any needed TARs.
	 */
	public void sendAvailableWork() {
		Optional<Task> workOptional = dispatcher.getWork(this);

		if (workOptional.isEmpty()) {
			connection.send(
				new RequestRunReply(false, null, false, null).asPacket(serializer)
			);
			return;
		}
		Task task = workOptional.get();
		myCurrentTask.set(task);

		boolean benchRepoUpToDate = runnerInformation.get().getBenchHash()
			.map(it -> it.equals("current"))
			.orElse(false);
		connection.send(
			new RequestRunReply(!benchRepoUpToDate, "current", true, task.getId().getId())
				.asPacket(serializer)
		);

		if (!benchRepoUpToDate) {
			// TODO: Send bench repo
		}

		try (OutputStream outputStream = connection.createBinaryOutputStream()) {
			dispatcher.getQueue().transferTask(task.getId(), outputStream);
		} catch (PrepareTransferException e) {
			LOGGER.info("Failed to transfer repo to runner: Archiving failed", e);
			// This task is corrupted, we can not benchmark it.
			dispatcher.completeTask(prepareTransferFailed(task, Instant.now(), e));
		} catch (TransferException | IOException | NoSuchTaskException e) {
			LOGGER.info("Failed to transfer repo to runner: Sending failed", e);
			dispatcher.getQueue().abortTaskProcess(task.getId());
			connection.close(StatusCode.TRANSFER_FAILED);
		}
	}

	public Run prepareTransferFailed(Task task, Instant start, PrepareTransferException exception) {
		return new Run(
			new RunId(task.getId().getId()),
			task.getAuthor(),
			getRunnerName(),
			getRunnerInformation().getInformation(),
			start,
			Instant.now(),
			"Archiving failed. Error: " + exception.getMessage()
		);
	}

}
