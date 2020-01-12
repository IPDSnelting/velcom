package de.aaaaaaah.velcom.backend.runner;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.Repo;
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
import java.util.stream.Collectors;

/**
 * The main dispatcher routine.
 */
public class DispatcherImpl implements Dispatcher {

	private Collection<ActiveRunnerInformation> activeRunners;
	private RepoAccess repoAccess;

	/**
	 * Creates a new dispatcher.
	 *
	 * @param queue the queue to register to
	 * @param repoAccess the repo access to use for fetching repositories
	 */
	public DispatcherImpl(Queue queue, RepoAccess repoAccess) {
		this.repoAccess = repoAccess;
		this.activeRunners = Collections.newSetFromMap(new ConcurrentHashMap<>());

		queue.onSomethingAborted(task -> abort(task.getSecond(), task.getFirst()));
		queue.onSomethingAdded(task -> onQueueChanged());
	}

	@Override
	public void addRunner(ActiveRunnerInformation runnerInformation) {
		removeRunner(runnerInformation);

		runnerInformation.addStatusListener(status -> {
			if (status == RunnerStatusEnum.IDLE) {
				onFreeRunner(runnerInformation);
			}
			// Runner reconnected after an abort
			else if (status == RunnerStatusEnum.WORKING) {
				if (runnerInformation.getCurrentCommit().isEmpty()) {
					resetRunner(runnerInformation);
				}
			}
		});
		runnerInformation.addResultListener(
			results -> this.onResultsReceived(runnerInformation, results)
		);
		activeRunners.add(runnerInformation);
		System.out.println("Added a runner " + runnerInformation);
	}

	private void removeRunner(ActiveRunnerInformation activeRunner) {
		if (activeRunner.getRunnerInformation().isEmpty()) {
			// Can not do anything else
			activeRunners.remove(activeRunner);
			return;
		}
		String name = activeRunner.getRunnerInformation().get().getName();
		System.out.println("Removing runner with name " + name);
		activeRunners.removeIf(runner ->
			runner.getRunnerInformation()
				.map(RunnerInformation::getName)
				.map(name::equals)
				.orElse(false)
		);
	}

	@Override
	public void onQueueChanged() {
		// TODO: 13.12.19 Implement
		System.out.println("Queue changed, TODO me");
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

	private void resetRunner(ActiveRunnerInformation runner) {
		try {
			runner.getRunnerStateMachine().resetRunner("Abort requested");
		} catch (IOException e) {
			e.printStackTrace();
			// It is already disconnected, so force it now
			runner.getConnectionManager().disconnect();
		}
	}

	private void onResultsReceived(ActiveRunnerInformation runner, BenchmarkResults results) {
		System.out.println("Got work " + results + " from " + runner);
	}

	private void onFreeRunner(ActiveRunnerInformation runner) {
		System.out.println("Free runner! " + runner);

		if (runner.getRunnerInformation().isEmpty()) {
			System.err.println("Did not get any information about an active runner!");
			runner.getConnectionManager().disconnect();
			return;
		}

		// It executes too fast...
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String runnerCommitHash = runner.getRunnerInformation()
			.get()
			.getCurrentBenchmarkRepoHash()
			.orElse("");

		String actualCommitHash = repoAccess.getLatestBenchmarkRepoHash().getHash();

		try {
			if (!runnerCommitHash.equals(actualCommitHash)) {
				runner.getRunnerStateMachine().sendBenchmarkRepo(
					repoAccess::streamBenchmarkRepoArchive,
					actualCommitHash
				);
			}

			Commit commitToTransfer = getUnsafeNewDummyCommit();
			RunnerWorkOrder workOrder = new RunnerWorkOrder(
				commitToTransfer.getRepoId().getId(), commitToTransfer.getHash().getHash()
			);

			runner.getRunnerStateMachine().startWork(
				getUnsafeNewDummyCommit(),
				workOrder,
				outputStream -> repoAccess.streamNormalRepoArchive(commitToTransfer, outputStream)
			);

			System.out.println("Known runners: " + getKnownRunners());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// FIXME: 15.12.19 Delete this
	private Commit getUnsafeNewDummyCommit() {
		Optional<Repo> workRepo = repoAccess.getAllRepos()
			.stream()
			.filter(it -> it.getName().equals("test-work"))
			.findFirst();
		if (workRepo.isEmpty()) {
			throw new IllegalStateException("No 'test-work' repo found!");
		}
		return new Commit(
			null, repoAccess,
			workRepo.get().getId(), new CommitHash("2be75aa4edb1dbe322d28b2bc1bd9277fd9233cb"),
			List.of(), null, null, null, null, null
		);
	}
}
