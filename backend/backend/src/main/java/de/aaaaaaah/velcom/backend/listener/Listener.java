package de.aaaaaaah.velcom.backend.listener;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.GlobalConfig;
import de.aaaaaaah.velcom.backend.access.AccessLayer;
import de.aaaaaaah.velcom.backend.access.commit.BenchmarkStatus;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.Branch;
import de.aaaaaaah.velcom.backend.access.repo.BranchName;
import de.aaaaaaah.velcom.backend.access.repo.Repo;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A listener periodically checks if there are new commits on stored repositories and, if there are,
 * passes them to the queue.
 */
public class Listener {

	private static final Logger LOGGER = LoggerFactory.getLogger(Listener.class);

	private final RepoAccess repoAccess;
	private final CommitAccess commitAccess;
	private final Queue queue;

	private final ScheduledExecutorService executor;
	private final Lock lock = new ReentrantLock();

	private final UnknownCommitFinder unknownCommitFinder;

	/**
	 * Constructs a new listener instance.
	 *
	 * @param config the config where the listener gets the poll interval from
	 * @param accessLayer the access layer to get several access instances, that the listener
	 * 	needs, from
	 * @param queue the queue into which unknown commits will be inserted
	 */
	public Listener(GlobalConfig config, AccessLayer accessLayer, Queue queue) {
		this.repoAccess = accessLayer.getRepoAccess();
		this.commitAccess = accessLayer.getCommitAccess();
		this.queue = queue;

		long pollInterval = config.getPollInterval();

		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(this::update, 0, pollInterval, TimeUnit.SECONDS);

		unknownCommitFinder = new BreadthFirstSearchFinder();
	}

	private void update() {
		repoAccess.updateBenchmarkRepo();

		for (Repo repo : repoAccess.getAllRepos()) {
			try {
				checkForUnknownCommits(repo.getId());
			} catch (CommitSearchException e) {
				LOGGER.warn("Could not fetch updates for repo " + repo, e);
			}
		}
	}

	/**
	 * Checks for new commits on the specified repository and passes the new commits to the queue.
	 *
	 * @param repoId the id of the repository to check for
	 */
	public void checkForUnknownCommits(RepoId repoId) throws CommitSearchException {
		long start = System.currentTimeMillis();

		try {
			this.lock.lock();

			LOGGER.info("Checking for unknown commits on repo: {}", repoId);

			Repo repo = repoAccess.getRepo(repoId);

			repoAccess.fetchOrClone(repoId);

			if (!commitAccess.hasKnownCommits(repoId)) {
				// this repository does not have any known commits which means that it must be new
				// therefore only the first commit of each tracked branch is inserted into the queue
				// and all other commits that exist so far will be marked as known

				// (1): Mark all commits as known (NO_BENCHMARK_REQUIRED)
				List<BranchName> branches = repo.getTrackedBranches()
					.stream()
					.map(Branch::getName)
					.collect(toList());

				Collection<CommitHash> commits;
				try (Stream<Commit> commitStream = commitAccess.getCommitLog(
					repo.getId(), branches)) {

					commits = commitStream
						.map(Commit::getHash)
						.collect(Collectors.toUnmodifiableList());
				}

				commitAccess.setBenchmarkStatus(repoId, commits,
					BenchmarkStatus.NO_BENCHMARK_REQUIRED);

				// (2): Set last commit of each tracked branch to BENCHMARK_REQUIRED
				repo.getTrackedBranches()
					.stream()
					.map(Branch::getCommit)
					.forEach(queue::addTask);
			} else {
				// The repo already has some known commits so we need to be smart about it
				// Group all new commits across all tracked branches into this
				// list before inserting them into the queue
				List<Commit> allNewCommits = new ArrayList<>();

				// (1): Find new commits
				try {
					for (Branch trackedBranch : repo.getTrackedBranches()) {
						Collection<Commit> newCommits = unknownCommitFinder.find(
							commitAccess, trackedBranch
						);

						allNewCommits.addAll(newCommits);
					}
				} catch (IOException e) {
					throw new CommitSearchException(
						"failed to check for unknown commits in repo: " + repoId, e
					);
				}

				// (2): Add new commits to queue (in a sorted manner)
				// TODO: Check if sorting order is correct or if it needs to be reversed
				allNewCommits.sort(Comparator.comparing(Commit::getAuthorDate));
				allNewCommits.forEach(queue::addTask);
			}
		} catch (Exception e) {
			throw new CommitSearchException(repoId, e);
		} finally {
			this.lock.unlock();

			long end = System.currentTimeMillis();
			LOGGER.debug("checkForUnknownCommits({}) took {} ms", repoId.getId(), (end - start));
		}
	}

	/**
	 * Shuts the listener down.
	 */
	public void shutdown() {
		executor.shutdown(); // Disable new tasks from being submitted

		try {
			// Wait a while for existing tasks to terminate
			if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
				executor.shutdownNow(); // Cancel currently executing tasks

				// Wait a while for tasks to respond to being cancelled
				if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
					LOGGER.warn("Listener thread pool did not terminate!");
				}
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			executor.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

}
