package de.aaaaaaah.velcom.backend.listener;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.exceptions.RepoAccessException;
import de.aaaaaaah.velcom.backend.data.benchrepo.BenchRepo;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Repo;
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
		// TODO: 19.10.20 Use executor again
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
	@Timed(histogram = true)
	private void updateAllRepos() {
		LOGGER.debug("Updating all repos");

		// Update the bench repo
		LOGGER.debug("Updating bench repo");
		try {
			benchRepo.checkForUpdates();
		} catch (RepoAccessException e) {
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
	 * <p>
	 * This function is threadsafe and can be called at any time, for example from the API when a new
	 * repo is being added.
	 *
	 * @param repo the repository to update
	 * @return Returns true if the repo was fetched or cloned successfully. Returns false if the
	 * 	remote could not be reached or fetched/cloned from.
	 */
	@Timed(histogram = true)
	public synchronized boolean updateRepo(Repo repo) {
		LOGGER.info("Updating repo {}", repo);

		String repoDirName = repo.getId().getDirectoryName();
		boolean reclone = false;

		// Check whether the repo still exists
		// Check whether the repo is a valid git repo
		// Check whether the remote url is still correct
		// If any of the above checks fail, reclone the repo.
		try (Repository jgitRepo = repoStorage.acquireRepository(repoDirName)) {
			// Check if remote url is still correct
			String targetRemoteUrl = repo.getRemoteUrl().getUrl();
			String realRemoteUrl = jgitRepo.getConfig().getString("remote", "origin", "url");
			if (!targetRemoteUrl.equals(realRemoteUrl)) {
				throw new InvalidRemoteUrlException(realRemoteUrl, targetRemoteUrl);
			}

			// Fetch updates
			GuickCloning.getInstance().updateBareRepo(jgitRepo.getDirectory().toPath());

			// And finally, the reason why we're here
			updateDbFromJgitRepo(repo, jgitRepo);

		} catch (NoSuchRepositoryException e) {
			LOGGER.info("No repo {} found, cloning...", repo);
			reclone = true;

		} catch (RepositoryAcquisitionException e) {
			LOGGER.info("Failed to acquire repo {} (maybe damaged), recloning...", repo, e);
			reclone = true;

		} catch (InvalidRemoteUrlException e) {
			// TODO: 19.10.20 Maybe just change remote url instead of recloning the entire repo?
			// Shouldn't matter too much in any case, as this is expected to happen very rarely.
			LOGGER.info("Repo {} has wrong remote url, recloning...", repo);
			LOGGER.debug("real url: {}, target url: {}", e.getRealRemoteUrl(), e.getTargetRemoteUrl());
			reclone = true;

		} catch (CloneException e) {
			LOGGER.info("Failed to fetch repo {} (maybe damaged), recloning...", repo, e);
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
		databaseStorage.acquireWriteTransaction(db -> {
			DbUpdater dbUpdater = new DbUpdater(repo, jgitRepo, db);
			dbUpdater.update();
		});
	}
}
