package de.aaaaaaah.velcom.backend.listener;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

import de.aaaaaaah.velcom.backend.data.benchrepo.BenchRepo;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.listener.dbupdate.DbUpdater;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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
	private static final String QUEUE_AUTHOR = "Listener";
	private static final TaskPriority TASK_PRIORITY = TaskPriority.LISTENER;

	private final DatabaseStorage databaseStorage;
	private final RepoStorage repoStorage;

	private final RepoWriteAccess repoAccess;
	private final BenchRepo benchRepo;
	private final Queue queue;

	private final Duration pollInterval;

	private final ScheduledExecutorService executor;

	/**
	 * Constructs a new listener instance.
	 *
	 * @param databaseStorage used to read and write various kinds of data
	 * @param repoStorage used to manipulate locally cloned repos
	 * @param repoAccess used to read repo data
	 * @param benchRepo used to keep the bench repo up-to-date
	 * @param queue used to add new commits to the queue
	 * @param pollInterval the time the listener waits between updating its repos
	 */
	public Listener(DatabaseStorage databaseStorage, RepoStorage repoStorage,
		RepoWriteAccess repoAccess, BenchRepo benchRepo, Queue queue, Duration pollInterval) {

		this.databaseStorage = databaseStorage;
		this.repoStorage = repoStorage;

		this.repoAccess = repoAccess;
		this.benchRepo = benchRepo;
		this.queue = queue;

		this.pollInterval = pollInterval;

		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleWithFixedDelay(
			this::onUpdate,
			0,
			pollInterval.toSeconds(),
			TimeUnit.SECONDS
		);
	}

	/**
	 * This function is called regularly by the executor.
	 */
	private void onUpdate() {
		updateAllRepos();
		runAnalyze();
	}

	/**
	 * Re-fetch and update the bench repo and all repos that are in the db. Remove all local clones of
	 * repos that are no longer in the db and (re-)clone all repos that are not cloned or whose clones
	 * are broken.
	 */
	@Timed(histogram = true)
	public synchronized void updateAllRepos() {
		LOGGER.debug("Updating all repos");

		// Update the bench repo
		LOGGER.debug("Updating bench repo");
		try {
			boolean success = updateRepoVia(
				"benchrepo",
				benchRepo.getDirName(),
				benchRepo.getRemoteUrl(),
				repository -> {
					// No need to do anything here
				}
			);

			if (!success) {
				LOGGER.warn("Failed to fetch updates from benchmark repo");
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to fetch updates from benchmark repo", e);
		}

		List<Repo> allRepos = repoAccess.getAllRepos();

		// Remove all repos that don't exist any more
		LOGGER.debug("Deleting old repos");
		try {
			deleteOldRepos(allRepos);
		} catch (IOException e) {
			LOGGER.warn("Failed to delete old locally cloned repositories", e);
		}

		// Update all existing repos
		LOGGER.debug("Updating existing repos");
		for (Repo repo : allRepos) {
			try {
				if (!updateRepo(repo)) {
					LOGGER.warn("Failed to update repo {}", repo.getId());
				}
			} catch (Exception e) {
				LOGGER.warn("Failed to update repo {}", repo.getId(), e);
			}
		}
	}

	/**
	 * Run the ANALYZE command. It's relatively quick, so it should be fine to just run it everytime
	 * the listener runs.
	 */
	@Timed(histogram = true)
	private void runAnalyze() {
		LOGGER.debug("Running ANALYZE");

		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.dsl().execute("ANALYZE");
		}
	}

	/**
	 * Delete all repos that should not exist any more because they've been removed from the
	 * database.
	 *
	 * @param allRepos the list of repos to keep
	 * @throws IOException if listing or deleting repos goes wrong somewhere
	 */
	@Timed(histogram = true)
	private void deleteOldRepos(List<Repo> allRepos) throws IOException {
		Set<String> reposThatShouldExist = Stream.concat(
			Stream.of(benchRepo.getDirName()),
			allRepos.stream()
				.map(Repo::getId)
				.map(RepoId::getDirectoryName)
		).collect(toSet());

		HashSet<String> localRepos = repoStorage.getRepoDirectories().stream()
			.map(Path::getFileName)
			.map(Path::toString)
			.collect(toCollection(HashSet::new));

		localRepos.removeAll(reposThatShouldExist);
		// Now only the local repos that shouldn't exist remain.

		for (String repoName : localRepos) {
			LOGGER.info("Deleting old repo {}", repoName);
			repoStorage.deleteRepository(repoName);
		}
	}

	/**
	 * Pull a repo (or clone it if it doesn't exist yet), read its contents and update the database.
	 *
	 * <p> This function is threadsafe and can be called at any time, for example from the API when a
	 * new repo is being added.
	 *
	 * @param repo the repository to update
	 * @return Returns true if the repo was fetched or cloned successfully. Returns false if the
	 * 	remote could not be reached or fetched/cloned from.
	 */
	@Timed(histogram = true)
	public boolean updateRepo(Repo repo) {
		return updateRepoVia(
			repo.getName() + " (" + repo.getIdAsString() + ")",
			repo.getId().getDirectoryName(),
			repo.getRemoteUrlAsString(),
			jgitRepo -> updateDbFromJgitRepo(repo, jgitRepo)
		);
	}

	/**
	 * Abstracts away updating a repo so the same updating logic can be applied to the bench repo.
	 *
	 * @param repoName name of the repo, used only for logging
	 * @param repoDirName name of the repo's directory. This is not just a {@link RepoId} because
	 * 	this function should also work for the bench repo.
	 * @param targetRemoteUrl the remote url the repo should have. The repo is recloned if its actual
	 * 	remote url doesn't match this target remote url.
	 * @param jgitRepoAction this function will be called on the jgit {@link Repository} if cloning
	 * 	or recloning was successful
	 * @return true if the repo was successfully updated, false otherwise
	 */
	private synchronized boolean updateRepoVia(String repoName, String repoDirName,
		String targetRemoteUrl, Consumer<Repository> jgitRepoAction) {

		LOGGER.info("Updating repo {}", repoName);

		boolean reclone = false;

		// Check whether the repo still exists
		// Check whether the repo is a valid git repo
		// Check whether the remote url is still correct
		// If any of the above checks fail, reclone the repo.
		try (Repository jgitRepo = repoStorage.acquireRepository(repoDirName)) {
			// Check if remote url is still correct
			String realRemoteUrl = jgitRepo.getConfig().getString("remote", "origin", "url");
			if (!targetRemoteUrl.equals(realRemoteUrl)) {
				throw new InvalidRemoteUrlException(realRemoteUrl, targetRemoteUrl);
			}

			// Fetch updates
			GuickCloning.getInstance().updateBareRepo(jgitRepo.getDirectory().toPath());

			// And finally, the reason why we're here
			jgitRepoAction.accept(jgitRepo);

		} catch (NoSuchRepositoryException e) {
			LOGGER.info("No repo {} found, cloning...", repoName);
			reclone = true;

		} catch (RepositoryAcquisitionException e) {
			LOGGER.info("Failed to acquire repo {} (maybe damaged), recloning...", repoName, e);
			reclone = true;

		} catch (InvalidRemoteUrlException e) {
			// TODO: 19.10.20 Maybe just change remote url instead of recloning the entire repo?
			// Shouldn't matter too much in any case, as this is expected to happen very rarely.
			LOGGER.info("Repo {} has wrong remote url, recloning...", repoName);
			LOGGER.debug("real url: {}, target url: {}", e.getRealRemoteUrl(), e.getTargetRemoteUrl());
			reclone = true;

		} catch (CloneException e) {
			LOGGER.info("Failed to fetch repo {} (maybe damaged), recloning...", repoName, e);
			reclone = true;
		}

		if (reclone) {
			// Delete the repo, clone it again and try to update the db from the newly cloned repo.
			try {
				repoStorage.deleteRepository(repoDirName);
				repoStorage.addRepository(repoDirName, targetRemoteUrl);
				repoStorage.acquireRepository(repoDirName, jgitRepoAction::accept);

			} catch (Exception e) {
				LOGGER.warn("Recloning repo {} has failed, possibly because remote url is invalid",
					repoName, e);
				return false;
			}
		}

		return true;
	}

	private void updateDbFromJgitRepo(Repo repo, Repository jgitRepo) {
		List<CommitHash> toBeQueued = databaseStorage.acquireWriteTransaction(db -> {
			DbUpdater dbUpdater = new DbUpdater(repo, jgitRepo, db);
			return dbUpdater.update();
		});

		if (toBeQueued.isEmpty()) {
			LOGGER.debug("Adding no new commits to queue");
		} else {
			LOGGER.info("Adding " + toBeQueued.size() + " new commits to queue");
			// The commits are ordered from old to new, which means that the new commits will be
			// benchmarked first.
			queue.addCommits(QUEUE_AUTHOR, repo.getId(), toBeQueued, TASK_PRIORITY);
		}
	}
}
