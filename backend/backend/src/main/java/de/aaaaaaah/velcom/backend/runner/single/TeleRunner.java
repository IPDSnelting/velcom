package de.aaaaaaah.velcom.backend.runner.single;

import de.aaaaaaah.velcom.backend.access.archiveaccess.exceptions.TarRetrieveException;
import de.aaaaaaah.velcom.backend.access.archiveaccess.exceptions.TarTransferException;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.builder.NewRun;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.builder.RunBuilder;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.taskaccess.exceptions.NoSuchTaskException;
import de.aaaaaaah.velcom.backend.data.benchrepo.BenchRepo;
import de.aaaaaaah.velcom.backend.runner.Dispatcher;
import de.aaaaaaah.velcom.backend.runner.KnownRunner;
import de.aaaaaaah.velcom.backend.runner.KnownRunner.CompletedTask;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server side class for a runner.
 */
public class TeleRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(TeleRunner.class);
	/**
	 * Completed tasks are cached so we can continue to show output lines in the frontend even if the
	 * task has finished. This variable controls *how many* are ached.
	 */
	private static final int MAX_CACHED_COMPLETED_TASKS = 2;

	private final AtomicReference<GetStatusReply> runnerInformation;
	private final Queue<CompletedTask> lastResults;
	private final String runnerName;
	private final Serializer serializer;
	private final Dispatcher dispatcher;
	private final AtomicReference<Task> myCurrentTask;
	private final AtomicReference<Instant> workingSince;
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
		this.workingSince = new AtomicReference<>();
		this.lastPing = new AtomicReference<>();
		this.lastResults = new ArrayDeque<>();
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
		if (disposed) {
			throw new IllegalStateException("I am disposed");
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
		if (runnerInformation.get() == null) {
			LOGGER.debug("Passing runner '{}' on to dispatcher", getRunnerName());
			dispatcher.addRunner(this);
		}
		runnerInformation.set(reply);

		if (reply.getRunId().isPresent()) {
			TaskId taskId = new TaskId(reply.getRunId().get());
			boolean taskInProgress = dispatcher.getQueue().isTaskInProgress(taskId);
			// Task is no longer in the queue or no longer in progress
			if (!taskInProgress) {
				abort();
			}
		}

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
		getCurrentTask().ifPresent(task -> dispatcher.getQueue().abortTask(task.getId()));
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

		synchronized (lastResults) {
			return new KnownRunner(
				getRunnerName(),
				reply.getInfo(),
				reply.getVersionHash().orElse(null),
				reply.getStatus(),
				myCurrentTask.get(),
				!hasConnection(),
				workingSince.get(),
				reply.getLastOutputLines().orElse(null),
				new ArrayList<>(lastResults)
			);
		}
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
	private void abort() {
		myCurrentTask.set(null);
		workingSince.set(null);

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
		workingSince.set(null);

		Task task = myCurrentTask.get();
		if (task == null) {
			LOGGER.warn("{} has no task but got results :(", getRunnerName());
			// Somehow we have no task but a result, retry that commit. This should not happen, but if it
			// does err on the side of caution. Retry that task if possible, better to benchmark twice
			// than never
			dispatcher.getQueue().abortTask(new TaskId(resultReply.getRunId()));
			return;
		}

		synchronized (lastResults) {
			lastResults.offer(new CompletedTask(
				new TaskId(resultReply.getRunId()),
				runnerInformation.get().getLastOutputLines().orElse(null)
			));

			while (lastResults.size() > MAX_CACHED_COMPLETED_TASKS) {
				lastResults.poll();
			}
		}

		NewRun run;
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

	private NewRun handleUnsuccessful(GetResultReply resultReply, Task task) {
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

	private NewRun handleSuccessful(GetResultReply resultReply, Task task) {
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
						metric.getUnit().map(Unit::new).orElse(null),
						metric.getInterpretation().map(Interpretation::fromSharedRepresentation).orElse(null),
						metric.getError().get()
					);
				} else {
					// That get is okay, we check for metric.getError().isPresent()
					//noinspection OptionalGetWithoutIsPresent
					builder.addSuccessfulMeasurement(
						name,
						metric.getUnit().map(Unit::new).orElse(null),
						metric.getInterpretation().map(Interpretation::fromSharedRepresentation).orElse(null),
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
	public void prepareAndSendWork() {
		LOGGER.debug("Runner {} asks for work", getRunnerName());

		Optional<String> benchRepoHash = benchRepo.getCurrentHash().map(CommitHash::getHash);

		if (benchRepoHash.isEmpty()) {
			LOGGER.info(
				"Benchmark repo has no hash (yet?), dispatching temporarily stopped for {}.",
				getRunnerName()
			);
			return;
		}

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
		workingSince.set(Instant.now());

		boolean benchRepoUpToDate = runnerInformation.get().getBenchHash()
			.map(it -> it.equals(benchRepoHash.get()))
			.orElse(false);

		LOGGER.info("Sending {} to runner {}", task.getId().getId(), getRunnerName());

		connection.send(
			new RequestRunReply(
				!benchRepoUpToDate,
				benchRepoUpToDate ? null : benchRepoHash.get(),
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
		} catch (TarRetrieveException e) {
			LOGGER.info(
				"Failed to transfer repo to runner " + getRunnerName() + ": Archiving failed", e
			);
			// This task is corrupted, we can not benchmark it.
			dispatcher.completeTask(tarRetrieveFailed(task, Instant.now(), e));
		} catch (TarTransferException | IOException | NoSuchTaskException e) {
			LOGGER.info(
				"Failed to transfer repo to runner " + getRunnerName() + ": Sending failed", e
			);
			dispatcher.getQueue().abortTask(task.getId());
			connection.close(StatusCode.TRANSFER_FAILED);
		}
	}

	private NewRun tarRetrieveFailed(Task task, Instant start, TarRetrieveException exception) {
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
			throws TarRetrieveException, TarTransferException, IOException, NoSuchTaskException;
	}

}
