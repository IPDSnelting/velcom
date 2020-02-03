package de.aaaaaaah.velcom.backend.access.repo;

import static java.util.stream.Collectors.toList;
import static org.jooq.codegen.db.tables.Repository.REPOSITORY;
import static org.jooq.codegen.db.tables.TrackedBranch.TRACKED_BRANCH;

import de.aaaaaaah.velcom.backend.access.AccessLayer;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.archive.ArchiveException;
import de.aaaaaaah.velcom.backend.access.repo.archive.Archiver;
import de.aaaaaaah.velcom.backend.access.repo.exception.AddRepoException;
import de.aaaaaaah.velcom.backend.access.repo.exception.DeleteRepoException;
import de.aaaaaaah.velcom.backend.access.repo.exception.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.access.repo.exception.RepoAccessException;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.AddRepositoryException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep2;
import org.jooq.Record1;
import org.jooq.codegen.db.tables.records.RepositoryRecord;
import org.jooq.codegen.db.tables.records.TrackedBranchRecord;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an abstraction for accessing the {@link Repo}s in the db.
 */
public class RepoAccess {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepoAccess.class);

	private final AccessLayer accessLayer;
	private final DatabaseStorage databaseStorage;
	private final RepoStorage repoStorage;
	private final Archiver archiver;

	private final String benchRepoDirName = "benchrepo";
	private final String benchRepoRemoteUrl;

	/**
	 * This constructor also registers the {@link RepoAccess} in the accessLayer.
	 *
	 * @param accessLayer the {@link AccessLayer} to register with
	 * @param databaseStorage a database storage
	 * @param repoStorage a repo storage
	 * @param benchRepoUrl the remote url of the benchmark repository
	 * @throws AddRepoException if an error occurs while trying to clone the benchmark repository
	 */
	public RepoAccess(AccessLayer accessLayer, DatabaseStorage databaseStorage,
		RepoStorage repoStorage, RemoteUrl benchRepoUrl) throws AddRepoException {

		this.accessLayer = accessLayer;
		this.databaseStorage = databaseStorage;
		this.repoStorage = repoStorage;
		this.archiver = new Archiver(repoStorage);

		// Clone benchmark repo if needed
		this.benchRepoRemoteUrl = benchRepoUrl.getUrl();

		if (!repoStorage.containsRepository(benchRepoDirName)) {
			try {
				repoStorage.addRepository(benchRepoDirName, benchRepoRemoteUrl);
			} catch (AddRepositoryException e) {
				throw new AddRepoException(e);
			}
		}

		// Clone all repos if needed
		for (Repo repo : getAllRepos()) {
			fetchOrClone(repo.getId());
		}

		accessLayer.registerRepoAccess(this);
	}

	// Repo operations

	/**
	 * Gets the repo instance by the given repo id.
	 *
	 * @param repoId the id of the repo
	 * @return the repo instance
	 * @throws NoSuchRepoException if no repo instance for the given repo id was found
	 */
	public Repo getRepo(RepoId repoId) throws NoSuchRepoException {
		try (DSLContext db = databaseStorage.acquireContext()) {
			RepositoryRecord nullableRecord = db.fetchOne(REPOSITORY,
				REPOSITORY.ID.eq(repoId.getId().toString()));

			if (nullableRecord == null) {
				throw new NoSuchRepoException(repoId);
			}

			return new Repo(
				this,
				accessLayer.getTokenAccess(),
				accessLayer.getBenchmarkAccess(),
				repoId
			);
		}
	}

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

		// (1): Clone repository (this may take a while)
		try {
			repoStorage.addRepository(repoId.getDirectoryName(), remoteUrl.getUrl());
		} catch (AddRepositoryException e) {
			throw new AddRepoException(e);
		}

		// (2): Insert repo into database
		try (DSLContext db = databaseStorage.acquireContext()) {
			RepositoryRecord record = db.newRecord(REPOSITORY);
			record.setId(repoId.getId().toString());
			record.setName(name);
			record.setRemoteUrl(remoteUrl.getUrl());
			record.insert();
		}

		// (3): Track branch that head points to
		try (Repository repo = repoStorage.acquireRepository(repoId.getDirectoryName())) {
			String defaultBranchStr = repo.getBranch();
			BranchName branchName = BranchName.fromName(defaultBranchStr);
			setTrackedBranches(repoId, List.of(branchName));
		} catch (RepositoryAcquisitionException | IOException e) {
			throw new AddRepoException(e);
		}

		return new Repo(
			this,
			accessLayer.getTokenAccess(),
			accessLayer.getBenchmarkAccess(),
			repoId
		);
	}

	/**
	 * Delete an existing repo.
	 *
	 * @param repoId the id of the repo to delete
	 * @throws DeleteRepoException if an error occurs while trying to delete the repo
	 */
	public void deleteRepo(RepoId repoId) throws DeleteRepoException {
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.deleteFrom(REPOSITORY)
				.where(REPOSITORY.ID.eq(repoId.getId().toString()))
				.execute();
		}

		try {
			repoStorage.deleteRepository(repoId.getDirectoryName());
		} catch (IOException e) {
			throw new DeleteRepoException(e);
		}
	}

	/**
	 * Performs either a fetch operation on the specified repository, or a clone operation if the
	 * repository has not been cloned to the local repo storage yet.
	 *
	 * @param repoId the id of the repository
	 * @throws RepoAccessException if an error occurs during the fetch/clone operation
	 */
	public void fetchOrClone(RepoId repoId) throws RepoAccessException {
		Repo repo = getRepo(repoId);
		try {
			fetchOrCloneLocalRepo(repoId.getDirectoryName(), repo.getRemoteUrl().getUrl());
		} catch (GitAPIException | RepositoryAcquisitionException | AddRepositoryException e) {
			throw new RepoAccessException(repoId, e);
		}
	}

	/**
	 * Performs either a fetch operation on the benchmark repository, or clones it to the local repo
	 * storage, if it has not yet been cloned.
	 *
	 * @throws RepoAccessException if an error occurs during the fetch/clone operation
	 */
	public void updateBenchmarkRepo() throws RepoAccessException {
		try {
			if (repoStorage.containsRepository(benchRepoDirName)) {
				// Check if remote url changed

				final String remoteUrl;

				try (Repository repo = repoStorage.acquireRepository(benchRepoDirName)) {
					remoteUrl = repo.getConfig().getString("remote", "origin", "url");
				}

				if (!remoteUrl.equals(this.benchRepoRemoteUrl)) {
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
						// benchmark repo was updated => remove old cloned archives since a new clone
						// will be created with the newest hash once it is required.
						archiver.deleteArchives(benchRepoDirName);
					}
				}
			} else {
				// bench repo is currently not on disk => clone it
				LOGGER.info("missing benchrepo on disk! cloning it...");
				fetchOrCloneLocalRepo(benchRepoDirName, benchRepoRemoteUrl);
			}
		} catch (GitAPIException | RepositoryAcquisitionException | AddRepositoryException | IOException e) {
			throw new RepoAccessException("failed to fetch/clone benchmark repo with remote url: "
				+ benchRepoRemoteUrl, e);
		}
	}

	private void fetchOrCloneLocalRepo(String dirName, String remoteUrl)
		throws GitAPIException, RepositoryAcquisitionException, AddRepositoryException {

		if (repoStorage.containsRepository(dirName)) {
			// local repo exists => just fetch
			LOGGER.info("fetching from {} into {}", remoteUrl, dirName);

			try (Repository repo = repoStorage.acquireRepository(dirName)) {
				Git.wrap(repo).fetch().call();
			}
		} else {
			// local repo does not exist => clone
			LOGGER.info("local repository {} is missing! cloning it from: {}", dirName, remoteUrl);
			repoStorage.addRepository(dirName, remoteUrl);
		}
	}

	// Mutable properties

	/**
	 * @param repoId the id of the repository
	 * @return the name of the repository associated with then given repo id.
	 */
	public String getName(RepoId repoId) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.select(REPOSITORY.NAME).from(REPOSITORY)
				.where(REPOSITORY.ID.eq(repoId.getId().toString()))
				.fetchOptional()
				.map(Record1::value1)
				.orElseThrow(() -> new NoSuchRepoException(repoId));
		}
	}

	/**
	 * Sets the name of the repository associated with the given repo id.
	 *
	 * @param repoId the id of the repository
	 * @param name the name
	 */
	public void setName(RepoId repoId, String name) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.update(REPOSITORY).set(REPOSITORY.NAME, name)
				.where(REPOSITORY.ID.eq(repoId.getId().toString()))
				.execute();
		}
	}

	/**
	 * Sets the name of the given repository.
	 *
	 * @param repo the repository
	 * @param name the name
	 */
	public void setName(Repo repo, String name) {
		setName(repo.getId(), name);
	}

	/**
	 * Gets the local repo id for the given repository associated with the given id.
	 *
	 * @param repoId the id of the repository
	 * @return the local repo id
	 */
	public RemoteUrl getRemoteUrl(RepoId repoId) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			String uriStr = db.select(REPOSITORY.REMOTE_URL)
				.from(REPOSITORY)
				.where(REPOSITORY.ID.eq(repoId.getId().toString()))
				.fetchOptional()
				.map(Record1::value1)
				.orElseThrow(() -> new NoSuchRepoException(repoId));

			return new RemoteUrl(uriStr);
		}
	}

	/**
	 * Sets the remote url of the repository associated with the given repo id.
	 *
	 * @param repoId the id of the repository
	 * @param remoteUrl the new remote url
	 */
	public void setRemoteUrl(RepoId repoId, RemoteUrl remoteUrl) throws RepoAccessException {
		// (1): Because this operation is quite expensive, check if remote url really changed
		RemoteUrl oldRemoteUrl = getRemoteUrl(repoId);
		if (oldRemoteUrl.equals(remoteUrl)) {
			return;
		}

		// (2): Update local repo
		try {
			repoStorage.deleteRepository(repoId.getDirectoryName());
			repoStorage.addRepository(repoId.getDirectoryName(), remoteUrl.getUrl());
		} catch (IOException | AddRepositoryException e) {
			throw new RepoAccessException(repoId, e);
		}

		// (3): Update database
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.update(REPOSITORY)
				.set(REPOSITORY.REMOTE_URL, remoteUrl.getUrl())
				.where(REPOSITORY.ID.eq(repoId.getId().toString()))
				.execute();
		}
	}

	/**
	 * Sets the remote url of the given repository.
	 *
	 * @param repo the repository
	 * @param remoteUrl the new remote url
	 */
	public void setRemoteUrl(Repo repo, RemoteUrl remoteUrl) {
		setRemoteUrl(repo.getId(), remoteUrl);
	}

	/**
	 * @param repoId the repo the branch is in
	 * @param branchName the name of the branch
	 * @return whether the branch is tracked. Defaults to false if no repo with that id exists
	 */
	public boolean isBranchTracked(RepoId repoId, BranchName branchName) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.selectCount().from(TRACKED_BRANCH)
				.where(TRACKED_BRANCH.REPO_ID.eq(repoId.getId().toString()))
				.and(TRACKED_BRANCH.BRANCH_NAME.eq(branchName.getName()))
				.fetchOne()
				.value1() == 1;
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
				InsertValuesStep2<TrackedBranchRecord, String, String> step = ts
					.insertInto(TRACKED_BRANCH, TRACKED_BRANCH.REPO_ID, TRACKED_BRANCH.BRANCH_NAME);
				branches.forEach(branchName -> step.values(repoIdStr, branchName.getName()));
				step.execute();
			});
		}
	}

	// Queries

	/**
	 * @param repoId the repo's id
	 * @return all of the repo's {@link Branch}es, including the untracked branches
	 * @throws RepoAccessException if an error occurs trying to read data from the repository
	 */
	public Collection<Branch> getBranches(RepoId repoId) throws RepoAccessException {
		String dirName = repoId.getDirectoryName();

		try (Repository jgitRepo = repoStorage.acquireRepository(dirName)) {
			List<Branch> branchList = new ArrayList<>();

			for (Ref branchRef : Git.wrap(jgitRepo).branchList().call()) {
				BranchName name = BranchName.fromFullName(branchRef.getName());

				Branch branch = new Branch(
					this,
					accessLayer.getCommitAccess(),
					repoId,
					name
				);

				branchList.add(branch);
			}

			return branchList;
		} catch (RepositoryAcquisitionException | GitAPIException e) {
			throw new RepoAccessException(repoId, e);
		}
	}

	/**
	 * @param repoId the repo's id
	 * @return all of the repo's tracked {@link Branch}es
	 */
	public Collection<Branch> getTrackedBranches(RepoId repoId) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.selectFrom(TRACKED_BRANCH)
				.where(TRACKED_BRANCH.REPO_ID.eq(repoId.getId().toString()))
				.fetchStream()
				.map(record -> new Branch(
					this,
					accessLayer.getCommitAccess(),
					repoId,
					BranchName.fromName(record.getBranchName())
				))
				.collect(toList());
		}
	}

	/**
	 * @return a collection of all tracked repositories
	 */
	public Collection<Repo> getAllRepos() {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.selectFrom(REPOSITORY)
				.fetchStream()
				.map(record -> new Repo(
					this,
					accessLayer.getTokenAccess(),
					accessLayer.getBenchmarkAccess(),
					new RepoId(UUID.fromString(record.getId()))
				))
				.collect(toList());
		}
	}

	/**
	 * @return the latest commit on the master branch of the benchmark repository
	 */
	public CommitHash getLatestBenchmarkRepoHash() throws RepoAccessException {
		return new CommitHash(getCommitHashByRef(benchRepoDirName, Constants.HEAD));
	}

	/**
	 * @param branch the branch
	 * @return returns the commit that the specified branch has pointed to
	 */
	public CommitHash getLatestCommitHash(Branch branch) throws RepoAccessException {
		return new CommitHash(getCommitHashByRef(
			branch.getRepoId().getDirectoryName(),
			branch.getName().getFullName()
		));
	}

	private String getCommitHashByRef(String dirName, String ref) throws RepoAccessException {
		try (Repository repo = repoStorage.acquireRepository(dirName)) {
			ObjectId refPtr = repo.resolve(ref);
			return refPtr.getName(); // returns sha-1 hash
		} catch (RepositoryAcquisitionException | IOException e) {
			throw new RepoAccessException("failed to access ref " + ref + " under local repo "
				+ dirName, e);
		}
	}

	// Archive

	/**
	 * Write an uncompressed tar archive containing the (recursively cloned) working directory for
	 * the specified commit to the output stream.
	 *
	 * @param commit the commit to send
	 * @param outputStream where to write the archive
	 * @throws ArchiveException if the commit could not be compressed (or something else went wrong
	 * 	during streaming)
	 */
	public void streamNormalRepoArchive(Commit commit, OutputStream outputStream)
		throws ArchiveException {

		String dirName = commit.getRepoId().getDirectoryName();
		archiver.archive(dirName, commit.getHash(), outputStream, false);
	}

	/**
	 * Does the same as {@link #streamNormalRepoArchive(Commit, OutputStream)}, but for the latest
	 * commit on the master branch in the benchmark repo.
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
