package de.aaaaaaah.velcom.backend.runner.single;

import de.aaaaaaah.velcom.backend.access.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.access.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunBuilder;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.entities.Unit;
import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.access.exceptions.PrepareTransferException;
import de.aaaaaaah.velcom.backend.access.exceptions.TransferException;
import de.aaaaaaah.velcom.backend.data.benchrepo.BenchRepo;
import de.aaaaaaah.velcom.backend.runner.Dispatcher;
import de.aaaaaaah.velcom.backend.runner.KnownRunner;
import de.aaaaaaah.velcom.backend.runner.single.state.AwaitAbortRunReply;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Benchmark;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Metric;
import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.AbortRun;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.RequestRunReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetResultReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetStatusReply;
import de.aaaaaaah.velcom.shared.util.ExceptionHelper;
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
	private final Serializer serializer;
	private final Dispatcher dispatcher;
	private final AtomicReference<Task> myCurrentTask;
	private final BenchRepo benchRepo;
	private final AtomicReference<Instant> lastPing;

	private volatile boolean disposed;

	private RunnerConnection connection;

	public TeleRunner(String runnerName, Serializer serializer, Dispatcher dispatcher,
		BenchRepo benchRepo) {
		this.runnerName = runnerName;
		this.serializer = serializer;
		this.dispatcher = dispatcher;
		this.benchRepo = benchRepo;
		this.runnerInformation = new AtomicReference<>();
		this.myCurrentTask = new AtomicReference<>();
		this.lastPing = new AtomicReference<>();
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
		connection = new RunnerConnection(serializer, this, lastPing);
		connection.addCloseListener(this::disposeConnection);

		lastPing.set(Instant.now());

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
	 * @return the time the last ping was received
	 */
	public Instant getLastPing() {
		return lastPing.get();
	}

	/**
	 * Disposes this TeleRunner. It can not be used again.
	 */
	public synchronized void dispose() {
		this.disposed = true;
		if (hasConnection()) {
			LOGGER.warn("Had a connection when I was disposed");
			this.connection.close(StatusCode.INTERNAL_ERROR);
		}
	}

	/**
	 * @return true if this TeleRunner is disposed and can not be used again
	 */
	public synchronized boolean isDisposed() {
		return disposed;
	}

	/**
	 * @return the runner information. This must always be non-null from the moment the runner is
	 * 	registered to the dispatcher. The value might be outdated.
	 */
	public KnownRunner getRunnerInformation() {
		GetStatusReply reply = runnerInformation.get();

		return new KnownRunner(
			getRunnerName(), reply.getInfo(), reply.getStatus(), myCurrentTask.get(), !hasConnection()
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
			LOGGER.info(
				"Tried to abort commit but was not connected with a runner: {}", getRunnerName()
			);
			return;
		}

		try {
			connection.getStateMachine()
				.switchFromRestingState(new AwaitAbortRunReply(this, connection));
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
			LOGGER.warn("{} has no task but got results :(", getRunnerName());
			// Somehow we have no task but a result, retry that commit. This should not happen, but if it
			// does err on the side of caution. Retry that task if possible, better to benchmark twice
			// than never
			dispatcher.getQueue().abortTaskProcess(new TaskId(resultReply.getRunId()));
			return;
		}

		Run run;
		if (resultReply.isSuccess()) {
			// the benchmark script exited normally
			run = handleSuccessful(resultReply, task);
		} else {
			// the benchmark script misbehaved
			run = handleUnsuccessful(resultReply, task);
		}
		dispatcher.completeTask(run);

		myCurrentTask.set(null);
	}

	private Run handleUnsuccessful(GetResultReply resultReply, Task task) {
		RunBuilder builder = RunBuilder.failed(
			task,
			getRunnerName(),
			getRunnerInformation().getInformation(),
			resultReply.getStartTime(),
			resultReply.getStopTime(),
			resultReply.getError().orElseThrow(),
			RunErrorType.BENCH_SCRIPT_ERROR
		);
		return builder.build();
	}

	private Run handleSuccessful(GetResultReply resultReply, Task task) {
		Result result = resultReply.getResult().orElseThrow();

		// Global error
		if (result.getError().isPresent()) {
			return RunBuilder.failed(
				task,
				getRunnerName(),
				getRunnerInformation().getInformation(),
				resultReply.getStartTime(),
				resultReply.getStopTime(),
				result.getError().get(),
				RunErrorType.BENCH_SCRIPT_ERROR
			).build();
		}

		// No global error, but maybe individual ones failed
		RunBuilder builder = RunBuilder.successful(
				task,
				getRunnerName(),
				getRunnerInformation().getInformation(),
				resultReply.getStartTime(),
				resultReply.getStopTime()
		);

		// That get is okay, we check for `getError().isPresent()
		//noinspection OptionalGetWithoutIsPresent
		for (Benchmark benchmark : result.getBenchmarks().get()) {
			for (Metric metric : benchmark.getMetrics()) {
				Dimension name = new Dimension(benchmark.getName(), metric.getName());
				if (metric.getError().isPresent()) {
					builder.addFailedMeasurement(
							name,
							new Unit(metric.getUnit()),
							Interpretation.fromSharedRepresentation(metric.getInterpretation()),
							metric.getError().get()
					);
				} else {
					// That get is okay, we check for metric.getError().isPresent()
					//noinspection OptionalGetWithoutIsPresent
					builder.addSuccessfulMeasurement(
							name,
							Interpretation.fromSharedRepresentation(metric.getInterpretation()),
							new Unit(metric.getUnit()),
							metric.getValues().get()
					);
				}
			}
		}

		return builder.build();
	}

	/**
	 * Sends a {@link RequestRunReply} and any needed TARs.
	 */
	public void sendAvailableWork() {
		LOGGER.debug("Runner {} asks for work", getRunnerName());
		Optional<Task> workOptional = Optional.ofNullable(myCurrentTask.get())
				.or(() -> dispatcher.getWork(this));

		if (workOptional.isEmpty()) {
			LOGGER.debug("Dispatcher gave me no work for {}", getRunnerName());
			connection.send(
					new RequestRunReply(false, null, false, null).asPacket(serializer)
			);
			return;
		}
		Task task = workOptional.get();
		myCurrentTask.set(task);

		String benchRepoHash = benchRepo.getCurrentHash().getHash();
		boolean benchRepoUpToDate = runnerInformation.get().getBenchHash()
			.map(it -> it.equals(benchRepoHash))
			.orElse(false);

		LOGGER.info("Sending {} to runner {}", task.getId().getId(), getRunnerName());

		connection.send(
			new RequestRunReply(
				!benchRepoUpToDate,
				benchRepoUpToDate ? null : benchRepoHash,
				true,
				task.getId().getId()
			)
				.asPacket(serializer)
		);

		if (!benchRepoUpToDate) {
			handleBinaryTransfer(task, benchRepo::transfer);
		}

		handleBinaryTransfer(
			task,
			outputStream -> dispatcher.getQueue().transferTask(task.getId(), outputStream)
		);
	}

	private void handleBinaryTransfer(Task task, TransferConsumer consumer) {
		try (OutputStream outputStream = connection.createBinaryOutputStream()) {
			consumer.accept(outputStream);
		} catch (PrepareTransferException e) {
			LOGGER.info(
				"Failed to transfer repo to runner " + getRunnerName() + ": Archiving failed", e
			);
			// This task is corrupted, we can not benchmark it.
			dispatcher.completeTask(prepareTransferFailed(task, Instant.now(), e));
		} catch (TransferException | IOException | NoSuchTaskException e) {
			LOGGER.info(
				"Failed to transfer repo to runner " + getRunnerName() + ": Sending failed", e
			);
			dispatcher.getQueue().abortTaskProcess(task.getId());
			connection.close(StatusCode.TRANSFER_FAILED);
		}
	}

	private Run prepareTransferFailed(Task task, Instant start,
		PrepareTransferException exception) {
		return RunBuilder.failed(
			task,
			getRunnerName(),
			getRunnerInformation().getInformation(),
			start,
			Instant.now(),
			"Archiving failed. Error:\n " + ExceptionHelper.getStackTrace(exception),
			RunErrorType.VELCOM_ERROR
		).build();
	}

	private interface TransferConsumer {

		void accept(OutputStream outputStream)
			throws PrepareTransferException, TransferException, IOException, NoSuchTaskException;
	}

}
