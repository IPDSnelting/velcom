package de.aaaaaaah.velcom.backend.listener;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.GlobalConfig;
import de.aaaaaaah.velcom.backend.newaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.KnownCommitWriteAccess;
import de.aaaaaaah.velcom.backend.newaccess.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.newaccess.entities.BenchmarkStatus;
import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.newaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
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
import org.jooq.meta.jaxb.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A listener periodically checks if there are new commits on stored repositories and, if there are,
 * passes them to the queue.
 */
public class Listener {

	private static final Logger LOGGER = LoggerFactory.getLogger(Listener.class);

	private final RepoWriteAccess repoAccess;
	private final CommitReadAccess commitAccess;
	private final KnownCommitWriteAccess knownCommitAccess;
	private final Queue queue;

	private final ScheduledExecutorService executor;
	private final Lock lock = new ReentrantLock();

	private final UnknownCommitFinder unknownCommitFinder;

	/**
	 * Constructs a new listener instance.
	 *
	 * @param config the config where the listener gets the poll interval from
	 * @param repoAccess used to read repo data
	 * @param commitAccess used to read commit data
	 * @param knownCommitAccess used to mark new commits as known
	 * @param queue the queue into which unknown commits will be inserted
	 */
	public Listener(GlobalConfig config, RepoWriteAccess repoAccess, CommitReadAccess commitAccess,
		KnownCommitWriteAccess knownCommitAccess, Queue queue) {
		this.repoAccess = repoAccess;
		this.commitAccess = commitAccess;
		this.knownCommitAccess = knownCommitAccess;
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
				checkForUnknownCommits(repo.getRepoId());
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

			repoAccess.updateRepo(repoId);

			if (!knownCommitAccess.hasKnownCommits(repoId)) {
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
					repo.getRepoId(), branches)) {

					commits = commitStream
						.map(Commit::getHash)
						.collect(Collectors.toUnmodifiableList());
				}

				knownCommitAccess.setBenchmarkStatus(repoId, commits,
					BenchmarkStatus.NO_BENCHMARK_REQUIRED);

				// (2): Set last commit of each tracked branch to BENCHMARK_REQUIRED
				repo.getTrackedBranches()
					.stream()
					.map(repoAccess::getLatestCommitHash)
					.map(commitHash -> commitAccess.getCommit(repoId, commitHash))
					.forEach(queue::addTask);
			} else {
				// The repo already has some known commits so we need to be smart about it
				// Group all new commits across all tracked branches into this
				// list before inserting them into the queue
				List<Commit> allNewCommits = new ArrayList<>();

				// (1): Find new commits
				try {
					for (Branch trackedBranch : repo.getTrackedBranches()) {
						CommitHash startCommitHash = repoAccess.getLatestCommitHash(trackedBranch);
						Commit startCommit = commitAccess.getCommit(repoId, startCommitHash);

						Collection<Commit> newCommits = unknownCommitFinder.find(
							commitAccess, knownCommitAccess, startCommit
						);

						allNewCommits.addAll(newCommits);
					}
				} catch (IOException e) {
					throw new CommitSearchException(
						"failed to check for unknown commits in repo: " + repoId, e
					);
				}

				// (2): Add new commits to queue (in a sorted manner)
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

}
