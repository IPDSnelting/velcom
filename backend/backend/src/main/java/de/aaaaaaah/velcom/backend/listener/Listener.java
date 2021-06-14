package de.aaaaaaah.velcom.backend.listener;

import static java.util.stream.Collectors.toSet;

import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.data.benchrepo.BenchRepo;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.listener.commits.DbUpdater;
import de.aaaaaaah.velcom.backend.listener.github.GithubApiError;
import de.aaaaaaah.velcom.backend.listener.github.GithubPrInteractor;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning.CloneException;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.NoSuchRepositoryException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import io.micrometer.core.annotation.Timed;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

	private final CommitReadAccess commitAccess;
	private final RepoWriteAccess repoAccess;
	private final BenchRepo benchRepo;
	private final Queue queue;

	private final Duration vacuumInterval;
	private Instant lastVacuum;

	private final String frontendUrl;

	// An explicit reference to the executor is kept to ensure it will never accidentally be garbage
	// collected. This might not be strictly necessary, but it doesn't hurt either.
	@SuppressWarnings("FieldCanBeLocal")
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
		CommitReadAccess commitAccess, RepoWriteAccess repoAccess, BenchRepo benchRepo, Queue queue,
		Duration pollInterval, Duration vacuumInterval, String frontendUrl) {

		this.databaseStorage = databaseStorage;
		this.repoStorage = repoStorage;

		this.commitAccess = commitAccess;
		this.repoAccess = repoAccess;
		this.benchRepo = benchRepo;
		this.queue = queue;

		this.vacuumInterval = vacuumInterval;
		this.lastVacuum = Instant.now();

		this.frontendUrl = frontendUrl;

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
		vacuumIfNecessary();
	}

	/**
	 * Re-fetch and update the bench repo and all repos that are in the db. Remove all local clones of
	 * repos that are no longer in the db and (re-)clone all repos that are not cloned or whose clones
	 * are broken.
	 */
	@Timed(histogram = true)
	public synchronized void updateAllRepos() {
		LOGGER.info("Updating all repos");

		tryToSynchronizeCommitsForBenchRepo();

		List<Repo> currentRepos = repoAccess.getAllRepos();
		tryToDeleteLocalReposExcept(currentRepos);

		for (Repo repo : currentRepos) {
			tryToUpdateRepo(repo);
		}
	}

	private void tryToSynchronizeCommitsForBenchRepo() {
		try {
			LOGGER.debug("Synchronizing bench repo");
			synchronizeCommitsForBenchRepo();
		} catch (Exception e) {
			LOGGER.warn("Failed to synchronize bench repo", e);
		}
	}

	private void synchronizeCommitsForBenchRepo() throws SynchronizeCommitsException {
		genericSynchronizeCommits(
			"benchrepo",
			benchRepo.getDirName(),
			benchRepo.getRemoteUrl(),
			jgitRepo -> {/* The bench repo commits are not stored in the db. */}
		);
	}

	private void tryToDeleteLocalReposExcept(List<Repo> reposToKeep) {
		try {
			LOGGER.debug("Cleaning up old repos");
			deleteLocalReposExcept(reposToKeep);
		} catch (IOException e) {
			LOGGER.warn("Failed to clean up old repos", e);
		}
	}

	private void deleteLocalReposExcept(List<Repo> reposToKeep) throws IOException {
		Set<String> reposThatShouldExist = Stream.concat(
			Stream.of(benchRepo.getDirName()),
			reposToKeep.stream()
				.map(Repo::getId)
				.map(RepoId::getDirectoryName)
		).collect(toSet());

		Set<String> reposThatShouldNotExist = repoStorage.getRepoDirectories().stream()
			.map(Path::getFileName)
			.map(Path::toString)
			.filter(name -> !reposThatShouldExist.contains(name))
			.collect(toSet());

		for (String repoName : reposThatShouldNotExist) {
			LOGGER.info("Deleting old repo {}", repoName);
			repoStorage.deleteRepository(repoName);
		}
	}

	private void tryToUpdateRepo(Repo repo) {
		try {
			LOGGER.debug("Updating repo {}", repo.getId());
			updateRepo(repo);
		} catch (GithubApiError e) {
			LOGGER.warn("Failed to update repo {}", repo.getId());
			LOGGER.warn("{}", e.getMessage());
		} catch (Exception e) {
			LOGGER.warn("Failed to update repo {}", repo.getId(), e);
		}
	}

	private void updateRepo(Repo repo)
		throws SynchronizeCommitsException, IOException, InterruptedException, URISyntaxException, GithubApiError {

		Optional<GithubPrInteractor> ghIntOpt = GithubPrInteractor
			.fromRepo(repo, databaseStorage, commitAccess, queue, frontendUrl);

		if (ghIntOpt.isPresent()) {
			GithubPrInteractor ghInteractor = ghIntOpt.get();

			ghInteractor.searchForNewPrCommands();

			synchronizeCommitsForRepo(repo);

			ghInteractor.markNewPrCommandsAsSeen();
			ghInteractor.addNewPrCommandsToQueue();
			ghInteractor.replyToFinishedPrCommands();
			ghInteractor.replyToErroredPrCommands();
		} else {
			synchronizeCommitsForRepo(repo);
		}
	}

	/**
	 * Fetch latest commits for repo, store them in the database and add them to the queue if
	 * necessary.
	 *
	 * @throws SynchronizeCommitsException if any of these steps didn't work. This might be because
	 * 	the remote url is invalid or inaccessible.
	 */
	@Timed(histogram = true)
	public void synchronizeCommitsForRepo(Repo repo) throws SynchronizeCommitsException {
		genericSynchronizeCommits(
			repo.getName() + " (" + repo.getIdAsString() + ")",
			repo.getId().getDirectoryName(),
			repo.getRemoteUrlAsString(),
			jgitRepo -> updateCommitsInDb(repo, jgitRepo)
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
	 * @throws SynchronizeCommitsException if the commits in the db could not be updated
	 * 	successfully. This may be because the remote url is invalid or inaccessible.
	 */
	private synchronized void genericSynchronizeCommits(
		String repoName,
		String repoDirName,
		String targetRemoteUrl,
		Consumer<Repository> jgitRepoAction
	) throws SynchronizeCommitsException {

		LOGGER.debug("Synchronizing commits for repo {}", repoName);

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
				throw new SynchronizeCommitsException();
			}
		}
	}

	private void updateCommitsInDb(Repo repo, Repository jgitRepo) {
		List<CommitHash> toBeQueued = databaseStorage.acquireWriteTransaction(db -> {
			return new DbUpdater(repo, jgitRepo, db).update();
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
	 * Run the VACUUM command. Compared to ANALYZE, this one may take a while and copy data around, so
	 * it is only ran occasionally.
	 */
	@Timed(histogram = true)
	private void vacuumIfNecessary() {
		Instant now = Instant.now();

		Duration timeSinceLastVacuum = Duration.between(lastVacuum, now);
		if (timeSinceLastVacuum.compareTo(vacuumInterval) >= 0) {
			LOGGER.debug("Running VACUUM");

			try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
				db.vacuum();
				lastVacuum = now;
			}
		}
	}
}
