package de.aaaaaaah.velcom.backend.runner;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The main dispatcher routine.
 */
public class DispatcherImpl implements Dispatcher {

	private Collection<ActiveRunnerInformation> activeRunners;

	/**
	 * Creates a new dispatcher.
	 *
	 * @param queue the queue to register to
	 */
	public DispatcherImpl(Queue queue) {
		this.activeRunners = Collections.newSetFromMap(new ConcurrentHashMap<>());
		queue.onSomethingAborted(task -> abort(task.getSecond(), task.getFirst()));
		queue.onSomethingAdded(task -> onQueueChanged());
	}

	@Override
	public void addRunner(ActiveRunnerInformation runnerInformation) {
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

	private void removeRunner(ActiveRunnerInformation runnerInformation) {
		System.out.println("Removing runner " + runnerInformation);
		activeRunners.remove(runnerInformation);
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

		// FIXME: 15.12.19 Implement
		String actualCommitHash = "hey";

		try {
			if (!runnerCommitHash.equals(actualCommitHash)) {
				runner.getRunnerStateMachine().sendBenchmarkRepo(
					outputStream -> getClass().getResourceAsStream("/runner-test-tmp/benchrepo.tar")
						.transferTo(outputStream),
					actualCommitHash
				);
			}

			runner.getRunnerStateMachine().startWork(
				getUnsafeNewDummyCommit(),
				new RunnerWorkOrder(UUID.randomUUID(), "hey", "test"),
				outputStream -> getClass().getResourceAsStream("/runner-test-tmp/work.tar")
					.transferTo(outputStream)
			);

			System.out.println("Known runners: " + getKnownRunners());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// FIXME: 15.12.19 Delete this
	private Commit getUnsafeNewDummyCommit() {
		return new Commit(
			null, null,
			new RepoId(UUID.randomUUID()), new CommitHash("hey"),
			List.of(), null, null, null, null, null
		);
	}
}
