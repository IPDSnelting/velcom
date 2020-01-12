package de.aaaaaaah.velcom.backend.runner;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.Branch;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * The main dispatcher routine.
 */
public class DispatcherImpl implements Dispatcher {

	// TODO: 12.01.20 Put commit back when runner crashes (ping failure) 

	private final Collection<ActiveRunnerInformation> activeRunners;
	private final Queue queue;
	private final RepoAccess repoAccess;
	private final java.util.Queue<ActiveRunnerInformation> freeRunners;
	private final ExecutorService dispatcherWorkerPool;

	/**
	 * Creates a new dispatcher.
	 *
	 * @param queue the queue to register to
	 * @param repoAccess the repo access to use for fetching repositories
	 */
	public DispatcherImpl(Queue queue, RepoAccess repoAccess) {
		this.queue = queue;
		this.repoAccess = repoAccess;
		this.activeRunners = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.freeRunners = new LinkedBlockingDeque<>();
		// TODO: 12.01.20 cached? Fixed size?
		this.dispatcherWorkerPool = Executors.newFixedThreadPool(5);

		queue.onSomethingAborted(task -> abort(task.getSecond(), task.getFirst()));
		queue.onSomethingAdded(task -> dispatcherWorkerPool.submit(this::updateDispatching));
	}

	@Override
	public void addRunner(ActiveRunnerInformation runnerInformation) {
		Optional<Commit> lastCommit = removeRunner(runnerInformation);

		AtomicBoolean initialConnect = new AtomicBoolean(true);

		runnerInformation.addStatusListener(status -> {
			// Runner reconnected while working
			if (initialConnect.getAndSet(false) && status == RunnerStatusEnum.WORKING) {
				// It should not be doing anything right now!
				if (lastCommit.isEmpty()) {
					resetRunner(runnerInformation);
				}
			}

			// TODO: 12.01.20 What to do with disconnected runners?
			// This listener here might be called from the websocket listener's
			// setState method if the connection is closed in onError while it is writing.
			if (status == RunnerStatusEnum.IDLE) {
				freeRunners.add(runnerInformation);
				dispatcherWorkerPool.submit(this::updateDispatching);
			}
		});
		runnerInformation.addResultListener(
			results -> this.onResultsReceived(runnerInformation, results)
		);
		activeRunners.add(runnerInformation);
		System.out.println("Added a runner " + runnerInformation);
	}

	/**
	 * Removes a runner, returning the commit it is working on.
	 *
	 * @param activeRunner the runner to remove
	 * @return the last commit it should be working on or an empty commit if the runner should be in
	 * 	idle mode
	 */
	private Optional<Commit> removeRunner(ActiveRunnerInformation activeRunner) {
		if (activeRunner.getRunnerInformation().isEmpty()) {
			// Can not do anything else
			activeRunners.remove(activeRunner);
			return Optional.empty();
		}
		String name = activeRunner.getRunnerInformation().get().getName();
		System.out.println("Removing runner with name " + name);
		List<ActiveRunnerInformation> matchingRunners = activeRunners.stream()
			.filter(runner ->
				runner.getRunnerInformation()
					.map(RunnerInformation::getName)
					.map(name::equals)
					.orElse(false)
			)
			.collect(Collectors.toList());

		// Delete them
		activeRunners.removeAll(matchingRunners);

		// This is confusing, two runners had the same name. Should not happen, so reset the new one
		if (matchingRunners.size() > 1) {
			return Optional.empty();
		}
		// Runner is new
		if (matchingRunners.isEmpty()) {
			return Optional.empty();
		}

		return matchingRunners.get(0).getCurrentCommit();
	}

	@Override
	public boolean abort(CommitHash commitHash, RepoId repoId) {
		System.out.println("Aborting " + commitHash + " " + repoId);
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
			System.out.println("Found a runner for it!");
			if (runner.getState() == RunnerStatusEnum.WORKING) {
				resetRunner(runner);
			}
			runner.setCurrentCommit(null);
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
		System.out.println("\n\nUpdating dispatching with runners:");
		for (ActiveRunnerInformation runner : freeRunners) {
			System.out.println("\t" + runner);
		}

		while (!freeRunners.isEmpty()) {
			ActiveRunnerInformation runner = freeRunners.poll();
			Optional<Commit> nextTask = queue.getNextTask();
			if (nextTask.isEmpty()) {
				nextTask = repoAccess.getAllRepos().stream()
					.filter(it -> it.getName().equals("test-work"))
					.findFirst()
					.flatMap(it -> it.getBranches()
						.stream()
						.filter(b -> b.getName().getName().endsWith("master"))
						.findFirst()
					)
					.map(Branch::getCommit);
				// TODO: 12.01.20 Replace this with the following line
				// return;
			}
			Commit commitToBenchmark = nextTask.get();
			if (!dispatchCommit(runner, commitToBenchmark)) {
				// TODO: 12.01.20 Re-Add it to queue
			}
		}
	}


	private void resetRunner(ActiveRunnerInformation runner) {
		System.out.println("Resetting runner " + runner);
		try {
			runner.getRunnerStateMachine().resetRunner("Abort requested");
			runner.setCurrentCommit(null);
		} catch (IOException e) {
			e.printStackTrace();
			// It is already disconnected, so force it now
			runner.getConnectionManager().disconnect();
		}
	}

	private void onResultsReceived(ActiveRunnerInformation runner, BenchmarkResults results) {
		System.out.println("Got work " + results + " from " + runner);
	}

	/**
	 * Dispatches a commit to the given runner. Directly marks the runner as working.
	 *
	 * @param runner the runner to dispatch it to
	 * @param commit the commit to benchmark
	 * @return true if the commit was dispatched, false otherwise
	 */
	private boolean dispatchCommit(ActiveRunnerInformation runner, Commit commit) {
		if (runner.getRunnerInformation().isEmpty()) {
			System.err.println("Did not get any information about an active runner!");
			runner.getConnectionManager().disconnect();
			return false;
		}

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

			runner.getRunnerStateMachine().startWork(
				commit,
				workOrder,
				outputStream -> repoAccess.streamNormalRepoArchive(commit, outputStream)
			);
			return true;
		} catch (IOException e) {
			System.err.println("Dispatching commit not possible :/ " + e.getMessage());
			return false;
		}
	}
}
