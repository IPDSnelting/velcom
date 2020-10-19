package de.aaaaaaah.velcom.backend.listener;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import de.aaaaaaah.velcom.backend.access.entities.Branch;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.exceptions.RepoAccessException;
import de.aaaaaaah.velcom.backend.access.policy.QueuePriority;
import de.aaaaaaah.velcom.backend.data.benchrepo.BenchRepo;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning.CloneException;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.NoSuchRepositoryException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import io.micrometer.core.annotation.Timed;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A listener periodically checks if there are new commits on stored repositories and, if there are,
 * passes them to the queue.
 */
public class Listener {

	private static final Logger LOGGER = LoggerFactory.getLogger(Listener.class);
	private static final String AUTHOR = "Listener";

	private final DatabaseStorage databaseStorage;
	private final RepoStorage repoStorage;

	private final RepoWriteAccess repoAccess;
	private final BenchRepo benchRepo;

	private final Duration pollInterval;

	/**
	 * Constructs a new listener instance.
	 *
	 * @param databaseStorage used to read and write various kinds of data
	 * @param repoStorage used to manipulate locally cloned repos
	 * @param repoAccess used to read repo data
	 * @param benchRepo used to keep the bench repo up-to-date
	 * @param pollInterval the time the listener waits between updating its repos
	 */
	public Listener(DatabaseStorage databaseStorage, RepoStorage repoStorage,
		RepoWriteAccess repoAccess, BenchRepo benchRepo, Duration pollInterval) {

		this.databaseStorage = databaseStorage;
		this.repoStorage = repoStorage;

		this.repoAccess = repoAccess;
		this.benchRepo = benchRepo;

		this.pollInterval = pollInterval;
	}

	/**
	 * Start the listener by launching the update thread.
	 */
	public void start() {
		LOGGER.debug("Starting listener");

		new Thread(() -> {
			try {
				LOGGER.info("Listener started");

				while (true) {
					updateAllRepos();
					Thread.sleep(pollInterval.toMillis());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	/**
	 * Update the bench repo and all repos that are in the db. Remove all local clones of repos that
	 * are no longer in the db and (re-)clone all repos that are not cloned or whose clones are
	 * broken.
	 */
	private void updateAllRepos() {
		// Update the bench repo
		try {
			benchRepo.checkForUpdates();
		} catch (RepoAccessException e) {
			LOGGER.warn("Failed to fetch updates from benchmark repo", e);
		}

		List<Repo> allRepos = repoAccess.getAllRepos();

		// Remove all repos that don't exist any more
		try {
			deleteOldRepos(allRepos);
		} catch (IOException e) {
			LOGGER.warn("Failed to delete old locally cloned repositories", e);
		}

		// Update all existing repos
		for (Repo repo : allRepos) {
			try {
				updateRepo(repo);
			} catch (Exception e) {
				LOGGER.warn("Failed to update repo {}", repo.getId(), e);
			}
		}
	}

	/**
	 * Delete all repos that should not exist any more because they've been removed from the
	 * database.
	 *
	 * @param allRepos the list of repos to keep
	 * @throws IOException if listing or deleting repos goes wrong somewhere
	 */
	private void deleteOldRepos(List<Repo> allRepos) throws IOException {
		Set<String> reposThatShouldExist = allRepos.stream()
			.map(Repo::getId)
			.map(RepoId::getDirectoryName)
			.collect(toSet());

		HashSet<String> localRepos = repoStorage.getRepoDirectories().stream()
			.map(Path::getFileName)
			.map(Path::toString)
			.collect(toCollection(HashSet::new));

		localRepos.removeAll(reposThatShouldExist);
		// Now only the local repos that shouldn't exist remain.

		for (String repoName : localRepos) {
			repoStorage.deleteRepository(repoName);
		}
	}

	/**
	 * Pull a repo (or clone it if it doesn't exist yet), read its contents and update the database.
	 * <p>
	 * This function is threadsafe and can be called at any time, for example from the API when a new
	 * repo is being added.
	 *
	 * @param repo the repository to update
	 * @return Returns true if the repo was fetched or cloned successfully. Returns false if the
	 * 	remote could not be reached or fetched/cloned from.
	 */
	public synchronized boolean updateRepo(Repo repo) {
		String repoDirName = repo.getId().getDirectoryName();
		boolean reclone = false;

		// Check whether the repo still exists
		// Check whether the repo is a valid git repo
		// Check whether the remote url is still correct
		// If any of the above checks fail, reclone the repo.
		try (Repository jgitRepo = repoStorage.acquireRepository(repoDirName)) {
			// Check if remote url is still correct
			String targetRemoteUrl = repo.getRemoteUrl().toString();
			String realRemoteUrl = jgitRepo.getConfig().getString("remote", "origin", "url");
			if (!targetRemoteUrl.equals(realRemoteUrl)) {
				throw new InvalidRemoteUrlException(realRemoteUrl, targetRemoteUrl);
			}

			// Fetch updates
			GuickCloning.getInstance().updateBareRepo(jgitRepo.getDirectory().toPath());

			// And finally, the reason why we're here
			updateDbFromJgitRepo(repo, jgitRepo);

		} catch (NoSuchRepositoryException e) {
			LOGGER.info("No repo {} found, cloning...", repo.getId(), e);
			reclone = true;

		} catch (RepositoryAcquisitionException e) {
			LOGGER.info("Failed to acquire repo {} (maybe damaged), recloning...", repo.getId(), e);
			reclone = true;

		} catch (InvalidRemoteUrlException e) {
			// TODO: 19.10.20 Maybe just change remote url instead of recloning the entire repo?
			// Shouldn't matter too much in any case, as this is expected to happen very rarely.
			LOGGER.info("Repo {} has wrong remote url, recloning...", repo.getId());
			reclone = true;

		} catch (CloneException e) {
			LOGGER.info("Failed to fetch repo {} (maybe damaged), recloning...", repo.getId(), e);
			reclone = true;
		}

		if (reclone) {
			// Delete the repo, clone it again and try to update the db from the newly cloned repo.
			try {
				repoStorage.deleteRepository(repoDirName);
				repoStorage.addRepository(repoDirName, repo.getRemoteUrl().getUrl());
				repoStorage.acquireRepository(repoDirName,
					jgitRepo -> updateDbFromJgitRepo(repo, jgitRepo));

			} catch (Exception e) {
				LOGGER.warn("Recloning repo {} has failed, possibly because remote url is invalid",
					repo.getId(), e);
				return false;
			}
		}

		return true;
	}

	private void updateDbFromJgitRepo(Repo repo, Repository jgitRepo) {
		// TODO: 19.10.20 implement (including db migration logic)
	}

	@Timed(histogram = true)
	private void update() {
		try {
			// TODO: 19.10.20 Check for remote url changes?
			benchRepo.checkForUpdates();
		} catch (RepoAccessException e) {
			LOGGER.warn("Could not fetch updates from benchmark repo!", e);
		}

		for (Repo repo : repoAccess.getAllRepos()) {
			try {
				updateRepo(repo.getRepoId());
			} catch (CommitSearchException | RepoAccessException | NoSuchRepoException e) {
				LOGGER.warn("Could not fetch updates for repo: " + repo, e);
			}
		}
	}

	public void updateRepo(RepoId repoId) throws CommitSearchException {
		LOGGER.info("Updating repo {}", repoId.getId());
		long start = System.currentTimeMillis();

		try {
			this.lock.lock();

			// No need to check whether the remote url has changed (like the bench repo does) because the
			// repo patch endpoint also updates and fetches the repo if the remote url is changed.

			repoAccess.updateRepo(repoId);

			pruneTrackedBranches(repoId);
			checkForUnknownCommits(repoId);
		} finally {
			this.lock.unlock();
		}

		long end = System.currentTimeMillis();
		LOGGER.debug("Updating repo {} took {} ms", repoId.getId(), (end - start));
	}

	/**
	 * Remove all tracked branches that don't actually exist in our repo.
	 *
	 * @param repoId the id of the repo whose tracked branches to update
	 */
	private void pruneTrackedBranches(RepoId repoId) {
		Repo repo = repoAccess.getRepo(repoId);

		Set<BranchName> existingBranches = repoAccess.getBranches(repo.getRepoId())
			.stream()
			.map(Branch::getName)
			.collect(toSet());

		Set<BranchName> trackedBranches = repo.getTrackedBranches().stream()
			.map(Branch::getName)
			.filter(existingBranches::contains)
			.collect(toSet());

		repoAccess.setTrackedBranches(repo.getRepoId(), trackedBranches);
	}

	/**
	 * Checks for new commits on the specified repository and passes the new commits to the queue.
	 *
	 * @param repoId the id of the repository to check for
	 */
	private void checkForUnknownCommits(RepoId repoId)
		throws CommitSearchException, RepoAccessException, NoSuchRepoException {

		Repo repo = repoAccess.getRepo(repoId);

		try {
			if (!knownCommitAccess.hasKnownCommits(repo.getRepoId())) {
				// this repository does not have any known commits which means that it must be new
				// therefore only the first commit of each tracked branch is inserted into the queue
				// and all other commits that exist so far will be marked as known

				// (1): Mark all commits as known (NO_BENCHMARK_REQUIRED)
				List<BranchName> branches = repoAccess.getBranches(repo.getRepoId())
					.stream()
					.map(Branch::getName)
					.collect(toList());

				Collection<CommitHash> commits;
				try (Stream<Commit> commitStream = commitAccess.getCommitLog(repo.getRepoId(), branches)) {
					commits = commitStream
						.map(Commit::getHash)
						.collect(Collectors.toUnmodifiableList());
				}

				knownCommitAccess.markCommitsAsKnown(repo.getRepoId(), commits);

				// (2): Make last commit of each tracked branch known
				List<CommitHash> latestHashes = repo.getTrackedBranches()
					.stream()
					.map(repoAccess::getLatestCommitHash)
					.collect(toList());

				List<Task> tasks = latestHashes.stream()
					.map(hash -> commitToTask(repo.getRepoId(), hash))
					.collect(toList());

				knownCommitAccess
					.markCommitsAsKnownAndInsertIntoQueue(repo.getRepoId(), latestHashes, tasks);
			} else {
				// The repo already has some known commits so we need to be smart about it
				// Group all new commits across all tracked branches into this
				// list before inserting them into the queue
				List<Commit> allNewCommits = new ArrayList<>();

				// (1): Find new commits
				try {
					for (Branch trackedBranch : repo.getTrackedBranches()) {
						CommitHash startCommitHash = repoAccess.getLatestCommitHash(trackedBranch);
						Commit startCommit = commitAccess.getCommit(repo.getRepoId(), startCommitHash);

						Collection<Commit> newCommits = unknownCommitFinder.find(
							commitAccess, knownCommitAccess, startCommit
						);

						allNewCommits.addAll(newCommits);
					}
				} catch (IOException e) {
					throw new CommitSearchException(
						"failed to check for unknown commits in repo: " + repo.getTrackedBranches(), e
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

				knownCommitAccess.markCommitsAsKnownAndInsertIntoQueue(repo.getRepoId(), hashes, tasks);
			}
		} catch (Exception e) {
			throw new CommitSearchException(repo.getRepoId(), e);
		}
	}

	private Task commitToTask(Commit commit) {
		return commitToTask(commit.getRepoId(), commit.getHash());
	}

	private Task commitToTask(RepoId repoId, CommitHash commitHash) {
		return new Task(
			AUTHOR,
			QueuePriority.LISTENER,
			new CommitSource(repoId, commitHash)
		);
	}

}
