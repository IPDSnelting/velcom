package de.aaaaaaah.velcom.backend.runner;

import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.benchmark.Interpretation;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.benchmark.RunBuilder;
import de.aaaaaaah.velcom.backend.access.benchmark.Unit;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.StatusCodeMappings;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults.Benchmark;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults.Metric;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
	private final RepoAccess repoAccess;
	private final BenchmarkAccess benchmarkAccess;
	private final Duration allowedRunnerDisconnectTime;
	private final java.util.Queue<ActiveRunnerInformation> freeRunners;
	private final ScheduledExecutorService watchdogPool;
	private final ExecutorService dispatcherExecutorPool;

	/**
	 * Creates a new dispatcher.
	 *
	 * @param queue the queue to register to
	 * @param repoAccess the repo access to use for fetching repositories
	 * @param benchmarkAccess the benchmark access to store results in
	 * @param allowedRunnerDisconnectTime the duration runners might be disconnected for before
	 * 	they are given up on (removed and commit rescheduled)
	 */
	public DispatcherImpl(Queue queue, RepoAccess repoAccess, BenchmarkAccess benchmarkAccess,
		Duration allowedRunnerDisconnectTime) {
		this.queue = queue;
		this.repoAccess = repoAccess;
		this.benchmarkAccess = benchmarkAccess;
		this.allowedRunnerDisconnectTime = allowedRunnerDisconnectTime;
		this.activeRunners = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.freeRunners = new ConcurrentLinkedQueue<>();
		this.watchdogPool = Executors.newSingleThreadScheduledExecutor();
		this.dispatcherExecutorPool = Executors.newFixedThreadPool(5);

		queue.onSomethingAborted(task -> abort(task.getSecond(), task.getFirst()));
		queue.onSomethingAdded(it -> dispatcherExecutorPool.submit(this::updateDispatching));

		watchdogPool.scheduleAtFixedRate(
			this::cleanupCrashedRunners,
			Math.max(allowedRunnerDisconnectTime.toSeconds() / 4, 1),
			Math.max(allowedRunnerDisconnectTime.toSeconds() / 4, 1),
			TimeUnit.SECONDS
		);
	}

	private void cleanupCrashedRunners() {
		StringJoiner checkedRunners = new StringJoiner(", ");
		for (ActiveRunnerInformation runner : activeRunners) {
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
		AtomicBoolean initialConnect = new AtomicBoolean(true);

		runnerInformation.setOnRunnerInformation(newInformation -> {
			boolean isWorking = newInformation.getRunnerState() == RunnerStatusEnum.WORKING;

			// the initialConnect is not needed atm, as the information is only transmitted at
			// startup. But that might change in the future and people will not look in this class
			// to fix it.
			boolean isInitialConnect = initialConnect.getAndSet(false);

			if (isInitialConnect) {
				// 1. Kick runners with same name, re-adding their commits to the queue
				// 2. Check if the runner says it is working but no commits were leftover from
				//    the previous runner incarnation (after a hard disconnect)
				// 3. If that was the case, reset the runner as the commit was probably aborted
				//    as the dispatcher does not know about it
				// 4. Add the runner to the managed activeRunners list

				List<Commit> lastCommits = disconnectRemoveRunnerByName(newInformation.getName());

				if (isWorking && lastCommits.isEmpty()) {
					resetRunner(runnerInformation);
				}
				activeRunners.add(runnerInformation);
			}

			LOGGER.info("Finished adding a runner {}.", newInformation);
		});
		runnerInformation.setOnIdle(() -> {
			freeRunners.add(runnerInformation);
			dispatcherExecutorPool.submit(this::updateDispatching);
		});
		runnerInformation.setOnDisconnected(value -> {
			if (value == StatusCodeMappings.CLIENT_ORDERLY_DISCONNECT) {
				LOGGER.debug("Runner exited properly. Removing it without grace period.");
				disconnectRemoveRunnerByInformation(runnerInformation);
			}
		});
		runnerInformation.setResultListener(
			results -> this.onResultsReceived(runnerInformation, results)
		);
		LOGGER.debug("Got add request for a runner {}", runnerInformation);
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
	public boolean abort(CommitHash commitHash, RepoId repoId) {
		LOGGER.debug("Aborting commit {} for repo {}!", commitHash, repoId);

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
				resetRunner(runner);
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


	private void resetRunner(ActiveRunnerInformation runner) {
		LOGGER.debug("Resetting runner {}", runner.getRunnerInformation());

		try {
			runner.getRunnerStateMachine().resetRunner("Abort requested");
		} catch (IOException e) {
			LOGGER.info("Failed to reset a runner", e);
			// It is already disconnected, so force it now
			runner.getConnectionManager().disconnect();
		}
	}

	private void onResultsReceived(ActiveRunnerInformation runner, BenchmarkResults results) {
		LOGGER.debug("Received runner results from {}", runner.getRunnerInformation());

		RepoId repoId = new RepoId(results.getWorkOrder().getRepoId());
		CommitHash commitHash = new CommitHash(results.getWorkOrder().getCommitHash());
		Instant startTime = results.getStartTime();
		Instant endTime = results.getEndTime();

		if (results.isError()) {
			RunBuilder runBuilder = RunBuilder.failed(
				repoId,
				commitHash,
				startTime,
				endTime,
				results.getError()
			);

			benchmarkAccess.addRun(runBuilder);
		} else {
			RunBuilder runBuilder = RunBuilder.successful(repoId, commitHash, startTime, endTime);

			for (Benchmark benchmark : results.getBenchmarks()) {
				for (Metric metric : benchmark.getMetrics()) {
					addMeasurementToRun(runBuilder, benchmark, metric);
				}
			}

			benchmarkAccess.addRun(runBuilder);
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
	 * @param commit the commit to benchmark
	 * @return true if the commit was dispatched, false otherwise
	 */
	private boolean dispatchCommit(ActiveRunnerInformation runner, Commit commit) {
		LOGGER.info("Dispatching {} on {}", commit, runner.getRunnerInformation());

		if (runner.getRunnerInformation().isEmpty()) {
			LOGGER.warn("Tried to dispatch a commit to a runner without information! ({})", commit);
			runner.getConnectionManager().disconnect();
			return false;
		}
		runner.getRunnerStateMachine().markAsMyCommit(commit);

		String runnerBenchmarkCommitHash = runner.getRunnerInformation()
			.get()
			.getCurrentBenchmarkRepoHash()
			.orElse("");

		String actualBenchmarkCommitHash = repoAccess.getLatestBenchmarkRepoHash().getHash();

		try {
			if (!runnerBenchmarkCommitHash.equals(actualBenchmarkCommitHash)) {
				runner.getRunnerStateMachine().sendBenchmarkRepo(
					repoAccess::streamBenchmarkRepoArchive,
					actualBenchmarkCommitHash
				);
			}

			RunnerWorkOrder workOrder = new RunnerWorkOrder(
				commit.getRepoId().getId(), commit.getHash().getHash()
			);

			// Commit was cancelled
			if (runner.getCurrentCommit().isEmpty()) {
				return true;
			}

			runner.getRunnerStateMachine().startWork(
				commit,
				workOrder,
				outputStream -> repoAccess.streamNormalRepoArchive(commit, outputStream)
			);
			return true;
		} catch (Throwable e) {
			LOGGER.info("Dispatching commit not possible", e);
			return false;
		}
	}
}
