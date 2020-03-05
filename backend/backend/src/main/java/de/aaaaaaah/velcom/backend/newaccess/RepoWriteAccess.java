package de.aaaaaaah.velcom.backend.newaccess;

import static java.util.stream.Collectors.toList;
import static org.jooq.codegen.db.tables.Repository.REPOSITORY;
import static org.jooq.codegen.db.tables.TrackedBranch.TRACKED_BRANCH;

import de.aaaaaaah.velcom.backend.newaccess.archive.ArchiveException;
import de.aaaaaaah.velcom.backend.newaccess.archive.Archiver;
import de.aaaaaaah.velcom.backend.newaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.newaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.newaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.exceptions.AddRepoException;
import de.aaaaaaah.velcom.backend.newaccess.exceptions.DeleteRepoException;
import de.aaaaaaah.velcom.backend.newaccess.exceptions.RepoAccessException;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning;
import de.aaaaaaah.velcom.backend.storage.repo.GuickCloning.CloneException;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.AddRepositoryException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.eclipse.jgit.lib.Repository;
import org.jooq.DSLContext;
import org.jooq.codegen.db.tables.records.RepositoryRecord;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides read and write access to tracked repositories.
 */
public class RepoWriteAccess extends RepoReadAccess {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepoWriteAccess.class);

	private final Archiver archiver;

	public RepoWriteAccess(DatabaseStorage databaseStorage, RepoStorage repoStorage,
		RemoteUrl benchRepoUrl, Path archivesRootDir) {

		super(databaseStorage, repoStorage, benchRepoUrl);

		this.archiver = new Archiver(repoStorage, archivesRootDir);

		// Clone benchmark repo if needed
		if (!repoStorage.containsRepository(benchRepoDirName)) {
			try {
				repoStorage.addRepository(benchRepoDirName, benchRepoRemoteUrl.getUrl());
			} catch (AddRepositoryException e) {
				throw new AddRepoException(benchRepoDirName, benchRepoRemoteUrl, e);
			}
		}

		// Clone all repos if needed
		for (RepoId repoId : getAllRepoIds()) {
			updateRepo(repoId);
		}
	}

	// --- Repo Setters ---------------------------------------------------------------------------

	/**
	 * Sets the name of the repository associated with the given repo id.
	 *
	 * @param repoId the id of the repository
	 * @param newName the new name
	 */
	public void setName(RepoId repoId, String newName) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.update(REPOSITORY).set(REPOSITORY.NAME, newName)
				.where(REPOSITORY.ID.eq(repoId.getId().toString()))
				.execute();
		}

		// Update cache
		final Repo cachedRepo = this.repoCache.getIfPresent(repoId);
		if (cachedRepo != null) {
			Repo newRepo = new Repo(
				cachedRepo.getRepoId(),
				newName,
				cachedRepo.getRemoteUrl(),
				cachedRepo.getTrackedBranches()
			);

			this.repoCache.put(newRepo.getRepoId(), newRepo);
		}
	}

	/**
	 * Sets the remote url of the repository associated with the given repo id.
	 *
	 * @param repoId the id of the repository
	 * @param newRemoteUrl the new remote url
	 */
	public void setRemoteUrl(RepoId repoId, RemoteUrl newRemoteUrl) {
		// (1): Because this operation is quite expensive, check if remote url really changed
		RemoteUrl oldRemoteUrl = getRemoteUrl(repoId);
		if (oldRemoteUrl.equals(newRemoteUrl)) {
			return;
		}

		// (2): Update local repo
		try {
			repoStorage.deleteRepository(repoId.getDirectoryName());
			repoStorage.addRepository(repoId.getDirectoryName(), newRemoteUrl.getUrl());
		} catch (IOException | AddRepositoryException e) {
			throw new RepoAccessException(repoId, e);
		}

		// (3): Update database
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.update(REPOSITORY)
				.set(REPOSITORY.REMOTE_URL, newRemoteUrl.getUrl())
				.where(REPOSITORY.ID.eq(repoId.getId().toString()))
				.execute();
		}

		// (4): Update cache
		final Repo cachedRepo = this.repoCache.getIfPresent(repoId);
		if (cachedRepo != null) {
			Repo newRepo = new Repo(
				cachedRepo.getRepoId(),
				cachedRepo.getName(),
				newRemoteUrl,
				cachedRepo.getTrackedBranches()
			);

			this.repoCache.put(newRepo.getRepoId(), newRepo);
		}
	}

	/**
	 * Set the repo's tracked branches. Ignores duplicate branches and invalid branches. All
	 * branches not inside the collection are set to untracked.
	 *
	 * @param repoId the repo's id
	 * @param branches the branches to be set as tracked
	 */
	public void setTrackedBranches(RepoId repoId, Collection<BranchName> branches) {
		String repoIdStr = repoId.getId().toString();

		try (DSLContext db = databaseStorage.acquireContext()) {
			db.transaction((configuration) -> {
				DSLContext ts = DSL.using(configuration);

				// Remove existing tracked branches
				ts.deleteFrom(TRACKED_BRANCH)
					.where(TRACKED_BRANCH.REPO_ID.eq(repoIdStr))
					.execute();

				// Add new tracked branches
				var insertStep = ts.insertInto(
					TRACKED_BRANCH, TRACKED_BRANCH.REPO_ID, TRACKED_BRANCH.BRANCH_NAME
				);

				branches.forEach(branchName -> insertStep.values(repoIdStr, branchName.getName()));

				insertStep.execute();
			});
		}

		// Update cache
		final Repo cachedRepo = this.repoCache.getIfPresent(repoId);
		if (cachedRepo != null) {
			Repo newRepo = new Repo(
				cachedRepo.getRepoId(),
				cachedRepo.getName(),
				cachedRepo.getRemoteUrl(),
				branches.stream().map(bName -> new Branch(repoId, bName)).collect(toList())
			);

			this.repoCache.put(newRepo.getRepoId(), newRepo);
		}
	}

	// --- Add / Delete Repos ---------------------------------------------------------------------

	/**
	 * Adds a new repository by cloning it to the local file system.
	 *
	 * @param name the name of the repository
	 * @param remoteUrl the remote url of the repository
	 * @return a new {@link Repo} instance
	 * @throws AddRepoException if an error occurs while trying to add the repository
	 */
	public Repo addRepo(String name, RemoteUrl remoteUrl) throws AddRepoException {
		RepoId repoId = new RepoId();

		// 1.) Clone repository (this may take a while)
		try {
			repoStorage.addRepository(repoId.getDirectoryName(), remoteUrl.getUrl());
		} catch (AddRepositoryException e) {
			throw new AddRepoException(name, remoteUrl, e);
		}

		// 2.) Insert repo into database
		try (DSLContext db = databaseStorage.acquireContext()) {
			RepositoryRecord record = db.newRecord(REPOSITORY);
			record.setId(repoId.getId().toString());
			record.setName(name);
			record.setRemoteUrl(remoteUrl.getUrl());
			record.insert();
		}

		// 3.) Track branch that head points to
		BranchName trackedBranchName;

		try (Repository repo = repoStorage.acquireRepository(repoId.getDirectoryName())) {
			String defaultBranchStr = repo.getBranch();
			trackedBranchName = BranchName.fromName(defaultBranchStr);
			setTrackedBranches(repoId, List.of(trackedBranchName));
		} catch (RepositoryAcquisitionException | IOException e) {
			throw new AddRepoException(name, remoteUrl, e);
		}

		// 4.) Create repo instance and cache it
		Repo repo = new Repo(
			repoId,
			name,
			remoteUrl,
			List.of(new Branch(repoId, trackedBranchName))
		);

		this.repoCache.put(repo.getRepoId(), repo);

		return repo;
	}

	/**
	 * Delete an existing repo.
	 *
	 * @param repoId the id of the repo to delete
	 * @throws DeleteRepoException if an error occurs while trying to delete the repo
	 */
	public void deleteRepo(RepoId repoId) throws DeleteRepoException {
		// Delete from database
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.deleteFrom(REPOSITORY)
				.where(REPOSITORY.ID.eq(repoId.getId().toString()))
				.execute();
		}

		// Delete from repo storage
		try {
			repoStorage.deleteRepository(repoId.getDirectoryName());
		} catch (IOException e) {
			throw new DeleteRepoException(repoId, e);
		}

		// Remove from cache
		this.repoCache.invalidate(repoId);
	}

	// --- Update Operations ----------------------------------------------------------------------

	/**
	 * Performs either a fetch operation on the specified repository, or a clone operation if the
	 * repository has not been cloned to the local repo storage yet.
	 *
	 * @param repoId the id of the repository
	 * @throws RepoAccessException if an error occurs during the fetch/clone operation
	 */
	public void updateRepo(RepoId repoId) throws RepoAccessException {
		try {
			RemoteUrl remoteUrl = getRemoteUrl(repoId);
			fetchOrCloneLocalRepo(repoId.getDirectoryName(), remoteUrl);
		} catch (RepositoryAcquisitionException | CloneException | AddRepositoryException e) {
			throw new RepoAccessException(repoId, e);
		}
	}

	/**
	 * Performs either a fetch operation on the benchmark repository, or clones it to the local repo
	 * storage, if it has not yet been cloned.
	 *
	 * @throws RepoAccessException if an error occurs during the fetch/clone operation
	 */
	public void updateBenchmarkRepo() {
		try {
			if (repoStorage.containsRepository(benchRepoDirName)) {
				// Check if remote url changed

				final String remoteUrl;

				try (Repository repo = repoStorage.acquireRepository(benchRepoDirName)) {
					remoteUrl = repo.getConfig().getString("remote", "origin", "url");
				}

				if (!remoteUrl.equals(this.benchRepoRemoteUrl.getUrl())) {
					// remote url changed! => delete repo so that fetchOrCloneLocalRepo
					// completely clones it again
					LOGGER.info("benchrepo remote url changed! cloning it again...");

					repoStorage.deleteRepository(benchRepoDirName);
					archiver.deleteArchives(benchRepoDirName);

					fetchOrCloneLocalRepo(benchRepoDirName, benchRepoRemoteUrl);
				} else {
					// remote url has not changed => just fetch and check if hash changed
					final CommitHash oldHash = getLatestBenchmarkRepoHash();
					fetchOrCloneLocalRepo(benchRepoDirName, benchRepoRemoteUrl);
					final CommitHash newHash = getLatestBenchmarkRepoHash();

					if (!oldHash.equals(newHash)) {
						// benchmark repo was updated => remove old cloned archives since a new
						// clone will be created with the newest hash once it is required.
						archiver.deleteArchives(benchRepoDirName);
					}
				}
			} else {
				// bench repo is currently not on disk => clone it
				LOGGER.info("missing benchrepo on disk! cloning it...");
				fetchOrCloneLocalRepo(benchRepoDirName, benchRepoRemoteUrl);
			}
		} catch (RepositoryAcquisitionException | AddRepositoryException | IOException |
			CloneException e) {

			throw new RepoAccessException("failed to fetch/clone benchmark repo with remote url: "
				+ benchRepoRemoteUrl, e);
		}
	}

	private void fetchOrCloneLocalRepo(String dirName, RemoteUrl remoteUrl)
		throws RepositoryAcquisitionException, AddRepositoryException, CloneException {

		if (repoStorage.containsRepository(dirName)) {
			// local repo exists => just fetch
			LOGGER.info("fetching from {} into {}", remoteUrl, dirName);

			try (Repository repo = repoStorage.acquireRepository(dirName)) {
				GuickCloning.getInstance().updateBareRepo(repo.getDirectory().toPath());
			}
		} else {
			// local repo does not exist => clone
			LOGGER.info("local repository {} is missing! cloning it from: {}", dirName, remoteUrl);
			repoStorage.addRepository(dirName, remoteUrl.getUrl());
		}
	}

	// --- Archive Operations ---------------------------------------------------------------------

	/**
	 * Write an uncompressed tar archive containing the (recursively cloned) working directory for
	 * the specified commit to the output stream.
	 *
	 * @param repoId the id of the repo
	 * @param commitHash the hash of commit to send
	 * @param outputStream where to write the archive
	 * @throws ArchiveException if the commit could not be compressed (or something else went wrong
	 * 	during streaming)
	 */
	public void streamNormalRepoArchive(RepoId repoId, CommitHash commitHash,
		OutputStream outputStream) throws ArchiveException {

		String dirName = repoId.getDirectoryName();
		archiver.archive(dirName, commitHash, outputStream, false);
	}

	/**
	 * Does the same as {@link #streamNormalRepoArchive(RepoId, CommitHash, OutputStream)}, but for
	 * the latest commit on the master branch in the benchmark repo.
	 *
	 * @param outputStream where to write the archive
	 * @throws ArchiveException if the commit could not be compressed (or something else went wrong
	 * 	during streaming)
	 */
	public void streamBenchmarkRepoArchive(OutputStream outputStream) throws ArchiveException {
		CommitHash commitHash = getLatestBenchmarkRepoHash();
		archiver.archive(this.benchRepoDirName, commitHash, outputStream, true);
	}

}
