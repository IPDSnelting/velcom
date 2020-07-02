package de.aaaaaaah.velcom.backend.runner;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import de.aaaaaaah.velcom.backend.ServerMain;
import de.aaaaaaah.velcom.backend.access.BenchmarkWriteAccess;
import de.aaaaaaah.velcom.backend.access.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.access.TaskWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.*;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.StatusCodeMappings;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults.Benchmark;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults.Metric;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main dispatcher routine.
 */
public class DispatcherImpl implements Dispatcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherImpl.class);

	private final Collection<ActiveRunnerInformation> activeRunners;
	private final Queue queue;
	private final TaskWriteAccess taskAccess;
	private final BenchmarkWriteAccess benchmarkAccess;
	private final Duration allowedRunnerDisconnectTime;
	private final java.util.Queue<ActiveRunnerInformation> freeRunners;
	@SuppressWarnings("FieldCanBeLocal") // Keep a strong reference to it
	private final ScheduledExecutorService watchdogPool;
	private final ExecutorService dispatcherExecutorPool;
	private final Histogram durationsHistogram;

	/**
	 * Creates a new dispatcher.
	 *
	 * @param queue the queue to register to
	 * @param taskAccess the task access used to transfer tasks to the runners
	 * @param benchmarkAccess the benchmark access to store results in
	 * @param allowedRunnerDisconnectTime the duration runners might be disconnected for before they
	 * 	are given up on (removed and commit rescheduled)
	 */
	public DispatcherImpl(Queue queue, TaskWriteAccess taskAccess,
		BenchmarkWriteAccess benchmarkAccess, Duration allowedRunnerDisconnectTime) {
		this.queue = queue;
		this.taskAccess = taskAccess;
		this.benchmarkAccess = benchmarkAccess;
		this.allowedRunnerDisconnectTime = allowedRunnerDisconnectTime;
		this.activeRunners = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.freeRunners = new ConcurrentLinkedQueue<>();
		this.watchdogPool = Executors.newSingleThreadScheduledExecutor();
		this.dispatcherExecutorPool = Executors.newFixedThreadPool(5);

		queue.onSomethingAborted(this::abort);
		queue.onSomethingAdded(it -> dispatcherExecutorPool.submit(this::updateDispatching));

		watchdogPool.scheduleAtFixedRate(
			() -> {
				LOGGER.info("Requesting update for runners");
				activeRunners.forEach(runner -> runner.getRunnerStateMachine().requestStatusUpdate());
			},
			10,
			10,
			TimeUnit.SECONDS
		);

		watchdogPool.scheduleAtFixedRate(
			this::cleanupCrashedRunners,
			Math.max(allowedRunnerDisconnectTime.toSeconds() / 4, 1),
			Math.max(allowedRunnerDisconnectTime.toSeconds() / 4, 1),
			TimeUnit.SECONDS
		);
		ServerMain.getMetricRegistry().register(
			MetricRegistry.name(getClass(), "runner_count"),
			(Gauge<Integer>) activeRunners::size
		);
		durationsHistogram = ServerMain.getMetricRegistry()
			.histogram(MetricRegistry.name(getClass(), "execution_durations"));
	}

	private void cleanupCrashedRunners() {
		StringJoiner checkedRunners = new StringJoiner(", ");
		for (ActiveRunnerInformation runner : activeRunners) {
			if (runner.getState() == RunnerStatusEnum.WORKING
				&& runner.getCurrentCommit().isEmpty()) {
				disconnectRemoveRunnerByInformation(runner);
				continue;
			}
			if (runner.getState() != RunnerStatusEnum.DISCONNECTED) {
				continue;
			}
			Duration timeSinceLastMessage = Duration.between(
				runner.getLastReceivedMessage(),
				Instant.now()
			);
			checkedRunners.add(
				runner.getRunnerInformation().map(RunnerInformation::getName).orElse(" ") + " with "
					+ timeSinceLastMessage
			);
			if (timeSinceLastMessage.compareTo(allowedRunnerDisconnectTime) > 0) {
				LOGGER.info(
					"Kicking inactive runner (Time since last message: {}) with information {}",
					timeSinceLastMessage, runner.getRunnerInformation()
				);
				disconnectRemoveRunnerByInformation(runner);
			}
		}
		LOGGER.info("Checked runners: {}", checkedRunners);
	}

	@Override
	public void addRunner(ActiveRunnerInformation runnerInformation) {
		if (runnerInformation.getRunnerInformation().isEmpty()) {
			throw new IllegalArgumentException("Runner info has no actual information");
		}

		RunnerInformation information = runnerInformation.getRunnerInformation().get();
		disconnectRemoveRunnerByName(information.getName()).forEach(queue::addTask);

		activeRunners.add(runnerInformation);
		runnerInformation.setOnDisconnected(value -> {
			if (value == StatusCodeMappings.CLIENT_ORDERLY_DISCONNECT) {
				LOGGER.debug("Runner exited properly. Removing it without grace period.");
				disconnectRemoveRunnerByInformation(runnerInformation);
			}
		});
		LOGGER.debug("Got add request for a runner {}", runnerInformation);
	}

	@Override
	public void runnerAvailable(ActiveRunnerInformation runnerInformation) {
		freeRunners.add(runnerInformation);
		dispatcherExecutorPool.submit(this::updateDispatching);
	}

	/**
	 * Removes the given runner and kicks it, re-adding the commits it was working on to the queue.
	 *
	 * @param information the runner information
	 */
	private void disconnectRemoveRunnerByInformation(ActiveRunnerInformation information) {
		information.getConnectionManager().disconnect();
		activeRunners.remove(information);

		information.getCurrentCommit().ifPresent(queue::addCommit);
	}

	/**
	 * Removes all runners with a given name and kicks them, re-adding the commits they were working
	 * on to the queue.
	 *
	 * @param name the name if the runner
	 * @return the commits that were assigned to removed runners
	 */
	private List<Commit> disconnectRemoveRunnerByName(String name) {
		List<ActiveRunnerInformation> matchingRunners = activeRunners.stream()
			.filter(runner ->
				runner.getRunnerInformation()
					.map(RunnerInformation::getName)
					.map(name::equals)
					.orElse(false)
			)
			.collect(Collectors.toList());

		freeRunners.removeAll(matchingRunners);
		// Delete them
		activeRunners.removeAll(matchingRunners);

		List<Commit> commits = new ArrayList<>();
		for (ActiveRunnerInformation runner : matchingRunners) {
			if (runner.getCurrentCommit().isPresent()) {
				commits.add(runner.getCurrentCommit().get());
			}
			LOGGER.info("Kicking runner as name '{}' was already taken!", name);
			runner.getConnectionManager().disconnect(
				StatusCodeMappings.NAME_ALREADY_TAKEN,
				"Name already taken!"
			);
		}

		return commits;
	}

	@Override
	public boolean abort(Task task) {
		LOGGER.debug("Aborting task {}!", task);

		for (ActiveRunnerInformation runner : activeRunners) {
			Optional<Commit> currentCommit = runner.getCurrentCommit();
			if (currentCommit.isEmpty()) {
				continue;
			}
			if (!currentCommit.get().getHash().equals(commitHash)) {
				continue;
			}
			if (!currentCommit.get().getRepoId().equals(repoId)) {
				continue;
			}
			LOGGER.debug(
				"Aborting {} for repo {} on runner {}",
				commitHash, repoId, runner.getRunnerInformation()
			);
			if (runner.getState() == RunnerStatusEnum.WORKING) {
				resetRunner(runner, "Abort requested");
			}
			runner.clearCurrentCommit();
			return true;
		}
		return false;
	}

	@Override
	public List<KnownRunner> getKnownRunners() {
		return activeRunners.stream()
			.filter(it -> it.getRunnerInformation().isPresent())
			.map(it -> KnownRunner.fromRunnerInformation(
				it.getRunnerInformation().get(),
				it.getCurrentCommit().orElse(null)
			))
			.collect(Collectors.toList());
	}

	private void updateDispatching() {
		LOGGER.debug(
			"Updating dispatching with {} free runners and a total of {}",
			freeRunners.size(), getKnownRunners().size()
		);

		ActiveRunnerInformation runner;
		while ((runner = freeRunners.poll()) != null) {
			Optional<Commit> nextTask = queue.getNextTask();
			if (nextTask.isEmpty()) {
				// no task for runner available, add it back to freeRunners
				freeRunners.add(runner);
				return;
			}

			Commit commitToBenchmark = nextTask.get();
			if (!dispatchCommit(runner, commitToBenchmark)) {
				queue.addCommit(commitToBenchmark);
			}
		}
	}


	private void resetRunner(ActiveRunnerInformation runner, String reason) {
		LOGGER.debug("Resetting runner {}", runner.getRunnerInformation());

		try {
			runner.getRunnerStateMachine().resetRunner(reason);
		} catch (IOException e) {
			LOGGER.info("Failed to reset a runner", e);
			// It is already disconnected, so force it now
			runner.getConnectionManager().disconnect();
		}
	}

	public void onResultsReceived(ActiveRunnerInformation runner, BenchmarkResults results) {
		LOGGER.debug("Received runner results from {}", runner.getRunnerInformation());

		RepoId repoId = new RepoId(results.getWorkOrder().getRepoId());
		CommitHash commitHash = new CommitHash(results.getWorkOrder().getCommitHash());
		Instant startTime = results.getStartTime();
		Instant endTime = results.getEndTime();

		durationsHistogram.update(Math.abs(ChronoUnit.SECONDS.between(startTime, endTime)));

		if (results.isError()) {
			RunBuilder runBuilder = RunBuilder.failed(
				repoId,
				commitHash,
				startTime,
				endTime,
				results.getError()
			);

			benchmarkAccess.insertRun(runBuilder.build());
		} else {
			RunBuilder runBuilder = RunBuilder.successful(repoId, commitHash, startTime, endTime);

			for (Benchmark benchmark : results.getBenchmarks()) {
				for (Metric metric : benchmark.getMetrics()) {
					addMeasurementToRun(runBuilder, benchmark, metric);
				}
			}

			benchmarkAccess.insertRun(runBuilder.build());
		}

		queue.finishTask(repoId, commitHash);
	}

	private void addMeasurementToRun(RunBuilder runBuilder, Benchmark benchmark, Metric metric) {
		MeasurementName measurementName = new MeasurementName(
			benchmark.getName(),
			metric.getName()
		);

		if (metric.isError()) {
			runBuilder.addFailedMeasurement(measurementName, metric.getError());
		} else {
			runBuilder.addSuccessfulMeasurement(
				measurementName,
				Interpretation.fromTextualRepresentation(
					metric.getResultInterpretation().name()
				),
				new Unit(metric.getUnit()),
				metric.getResults()
			);
		}
	}

	/**
	 * Dispatches a commit to the given runner. Directly marks the runner as working.
	 *
	 * @param runner the runner to dispatch it to
	 * @param task the task to dispatch
	 * @return true if the commit was dispatched, false otherwise
	 */
	private boolean dispatchTask(ActiveRunnerInformation runner, Task task) {
		LOGGER.info("Dispatching {} on {}", task, runner.getRunnerInformation());

		try {
			runner.getRunnerStateMachine().dispatchTask(task);
			return true;
		} catch (Throwable e) {
			LOGGER.info("Dispatching commit not possible", e);
			return false;
		}
	}
}
