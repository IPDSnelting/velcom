package de.aaaaaaah.velcom.backend.listener;

import static java.util.stream.Collectors.toList;

import com.codahale.metrics.Histogram;
import de.aaaaaaah.velcom.backend.GlobalConfig;
import de.aaaaaaah.velcom.backend.ServerMain;
import de.aaaaaaah.velcom.backend.access.entities.*;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.KnownCommitWriteAccess;
import de.aaaaaaah.velcom.backend.access.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.access.exceptions.RepoAccessException;
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
	private static final String AUTHOR = "Listener";

	private final RepoWriteAccess repoAccess;
	private final CommitReadAccess commitAccess;
	private final KnownCommitWriteAccess knownCommitAccess;

	private final ScheduledExecutorService executor;
	private final Lock lock = new ReentrantLock();

	private final UnknownCommitFinder unknownCommitFinder;

	private final Histogram updateDurations = ServerMain.getMetricRegistry()
		.histogram("listener-update-dur");

	/**
	 * Constructs a new listener instance.
	 *
	 * @param config the config where the listener gets the poll interval from
	 * @param repoAccess used to read repo data
	 * @param commitAccess used to read commit data
	 * @param knownCommitAccess used to mark new commits as known
	 */
	public Listener(GlobalConfig config, RepoWriteAccess repoAccess, CommitReadAccess commitAccess,
		KnownCommitWriteAccess knownCommitAccess) {
		this.repoAccess = repoAccess;
		this.commitAccess = commitAccess;
		this.knownCommitAccess = knownCommitAccess;

		long pollInterval = config.getPollInterval();

		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(this::update, 0, pollInterval, TimeUnit.SECONDS);

		unknownCommitFinder = new BreadthFirstSearchFinder();
	}

	private void update() {
		long start = System.currentTimeMillis();

		try {
			repoAccess.updateBenchmarkRepo();
		} catch (RepoAccessException e) {
			LOGGER.warn("Could not fetch updates from benchmark repo!", e);
		}

		for (Repo repo : repoAccess.getAllRepos()) {
			try {
				checkForUnknownCommits(repo.getRepoId());
			} catch (CommitSearchException | RepoAccessException | NoSuchRepoException e) {
				LOGGER.warn("Could not fetch updates for repo: " + repo, e);
			}
		}

		long end = System.currentTimeMillis();
		updateDurations.update(end - start);
	}

	/**
	 * Checks for new commits on the specified repository and passes the new commits to the queue.
	 *
	 * @param repoId the id of the repository to check for
	 */
	public void checkForUnknownCommits(RepoId repoId)
		throws CommitSearchException, RepoAccessException, NoSuchRepoException {

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
				List<BranchName> branches = repoAccess.getBranches(repoId)
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

				knownCommitAccess.markCommitsAsKnown(repoId, commits);

				// (2): Make last commit of each tracked branch known
				List<CommitHash> latestHashes = repo.getTrackedBranches()
					.stream()
					.map(repoAccess::getLatestCommitHash)
					.collect(toList());

				List<Task> tasks = latestHashes.stream()
					.map(hash -> commitToTask(repoId, hash))
					.collect(toList());

				knownCommitAccess.markCommitsAsKnownAndInsertIntoQueue(repoId, latestHashes, tasks);
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

				List<CommitHash> hashes = allNewCommits.stream()
					.map(Commit::getHash)
					.collect(toList());

				List<Task> tasks = allNewCommits.stream()
					.map(this::commitToTask)
					.collect(toList());

				knownCommitAccess.markCommitsAsKnownAndInsertIntoQueue(repoId, hashes, tasks);
			}
		} catch (Exception e) {
			throw new CommitSearchException(repoId, e);
		} finally {
			this.lock.unlock();

			long end = System.currentTimeMillis();
			LOGGER.debug("checkForUnknownCommits({}) took {} ms", repoId.getId(), (end - start));
		}
	}

	private Task commitToTask(Commit commit) {
		return commitToTask(commit.getRepoId(), commit.getHash());
	}

	private Task commitToTask(RepoId repoId, CommitHash commitHash) {
		return new Task(AUTHOR, new RepoSource(repoId, commitHash));
	}

}
