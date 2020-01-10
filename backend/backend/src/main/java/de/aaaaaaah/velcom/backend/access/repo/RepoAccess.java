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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.codegen.db.tables.records.RepositoryRecord;
import org.jooq.codegen.db.tables.records.TrackedBranchRecord;

/**
 * This class is an abstraction for accessing the {@link Repo}s in the db.
 */
public class RepoAccess {

	private final AccessLayer accessLayer;
	private final DatabaseStorage databaseStorage;
	private final RepoStorage repoStorage;
	private final Archiver archiver;

	private final String benchRepoDirName = "benchrepo";

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
		RepoStorage repoStorage, URI benchRepoUrl) throws AddRepoException {

		this.accessLayer = accessLayer;
		this.databaseStorage = databaseStorage;
		this.repoStorage = repoStorage;
		this.archiver = new Archiver(repoStorage);

		// Clone benchmark repo if needed
		//if (!repoStorage.containsRepository(benchRepoDirName)) {
		//	try {
		//		repoStorage.addRepository(benchRepoDirName, benchRepoUrl);
		//	} catch (AddRepositoryException e) {
		//		throw new AddRepoException(e);
		//	}
		//}

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
	public Repo addRepo(String name, URI remoteUrl) throws AddRepoException {

		RepoId repoId = new RepoId();

		// (1): Clone repository (this may take a while)
		try {
			repoStorage.addRepository(repoId.getDirectoryName(), remoteUrl);
		} catch (AddRepositoryException e) {
			throw new AddRepoException(e);
		}

		// (2): Insert repo into database
		try (DSLContext db = databaseStorage.acquireContext()) {
			RepositoryRecord record = db.newRecord(REPOSITORY);
			record.setId(repoId.getId().toString());
			record.setName(name);
			record.setRemoteUrl(remoteUrl.toString());
			record.insert();
		}

		// (3): Find master branch and set it to tracked
		// TODO: implement

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
	public URI getRemoteUrl(RepoId repoId) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			String uriStr = db.select(REPOSITORY.REMOTE_URL)
				.from(REPOSITORY)
				.where(REPOSITORY.ID.eq(repoId.getId().toString()))
				.fetchOptional()
				.map(Record1::value1)
				.orElseThrow(() -> new NoSuchRepoException(repoId));

			try {
				return new URI(uriStr);
			} catch (URISyntaxException e) {
				throw new IllegalStateException("remote url is invalid: " + uriStr);
			}
		}
	}

	/**
	 * Sets the local repo id of the repository associated with the given repo id.
	 *
	 * @param repoId the id of the repository
	 * @param remoteUrl the new remote url
	 */
	public void setRemoteUrl(RepoId repoId, URI remoteUrl) {
		// (1): Update local repo
		// TODO implement

		// (2): Update database
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.update(REPOSITORY)
				.set(REPOSITORY.REMOTE_URL, remoteUrl.toString())
				.where(REPOSITORY.ID.eq(repoId.getId().toString()))
				.execute();
		}
	}

	/**
	 * Sets the local repo id of the given repository.
	 *
	 * @param repo the repository
	 * @param remoteUrl the new remote url
	 */
	public void setLocalRepoId(Repo repo, URI remoteUrl) {
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
	 * Marks the given branch inside the given repository as tracked.
	 *
	 * @param repoId the repo the branch is in
	 * @param branchName the name of the branch
	 * @param tracked the branch's new tracked state
	 */
	public void setBranchTracked(RepoId repoId, BranchName branchName, boolean tracked) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			TrackedBranchRecord record = db.newRecord(TRACKED_BRANCH);
			record.setRepoId(repoId.getId().toString());
			record.setBranchName(branchName.getName());
			record.insert();
		}
	}

	/**
	 * A helper function for {@link #setBranchTracked(RepoId, BranchName, boolean)}.
	 *
	 * @param branch the branch
	 * @param tracked the branch's new tracked state
	 */
	public void setBranchTracked(Branch branch, boolean tracked) {
		setBranchTracked(branch.getRepoId(), branch.getName(), tracked);
	}

	// Queries

	/**
	 * @param repoId the repo's id
	 * @return all of the repo's {@link Branch}es, including the untracked branches
	 * @throws RepoAccessException if an error occurs trying to read data from the repository
	 */
	public Collection<Branch> getBranches(RepoId repoId) throws RepoAccessException {
		Repo repo = getRepo(repoId);
		String dirName = repoId.getDirectoryName();

		try (Repository jgitRepo = repoStorage.acquireRepository(dirName)) {
			List<Branch> branchList = new ArrayList<>();

			for (Ref branchRef : Git.wrap(jgitRepo).branchList().call()) {
				BranchName name = new BranchName(branchRef.getName());

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
			throw new RepoAccessException(repoId);
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
					new BranchName(record.getBranchName())
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

	/**
	 * @return the latest commit on the master branch of the benchmark repository
	 */
	public CommitHash getLatestBenchmarkRepoHash() {
		return null;
	}

	/**
	 * @param branch the branch
	 * @return returns the commit that the specified branch has pointed to
	 */
	public CommitHash getLatestCommitHash(Branch branch) {
		return null;
	}

	/**
	 * Performs either fetch operation on the specified repository, or a clone operation if the
	 * repository has not been cloned to the local repo storage yet.
	 *
	 * @param repoId the id of the repository
	 */
	public void fetch(RepoId repoId) {
	}


}
