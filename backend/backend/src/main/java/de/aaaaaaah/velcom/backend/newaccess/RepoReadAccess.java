package de.aaaaaaah.velcom.backend.newaccess;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.Tables.REPOSITORY;
import static org.jooq.codegen.db.Tables.TRACKED_BRANCH;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.aaaaaaah.velcom.backend.newaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.newaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.newaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.newaccess.exceptions.RepoAccessException;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.NoSuchRepositoryException;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.codegen.db.tables.records.RepositoryRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides read access to tracked repositories.
 */
public class RepoReadAccess {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepoReadAccess.class);

	protected final DatabaseStorage databaseStorage;
	protected final RepoStorage repoStorage;

	protected final String benchRepoDirName = "benchrepo";
	protected final RemoteUrl benchRepoRemoteUrl;

	protected final Cache<RepoId, Repo> repoCache = Caffeine.newBuilder()
		.maximumSize(100)
		.build();

	public RepoReadAccess(DatabaseStorage databaseStorage, RepoStorage repoStorage,
		RemoteUrl benchRepoUrl) {

		this.databaseStorage = databaseStorage;
		this.repoStorage = repoStorage;

		this.benchRepoRemoteUrl = benchRepoUrl;
	}

	// --- Repo Getters ---------------------------------------------------------------------------

	/**
	 * Gets the repository with the specified id.
	 *
	 * @param repoId the id of the repository
	 * @return the repository
	 * @throws NoSuchRepoException if no repo with that id exists
	 */
	public Repo getRepo(RepoId repoId) throws NoSuchRepoException {
		// Check cache
		final Repo cachedRepo = this.repoCache.getIfPresent(repoId);
		if (cachedRepo != null) {
			return cachedRepo;
		}

		// Check database
		RepositoryRecord repoRecord;

		try (DSLContext db = databaseStorage.acquireContext()) {
			repoRecord = db.fetchOne(REPOSITORY,
				REPOSITORY.ID.eq(repoId.getId().toString())
			);

			if (repoRecord == null) {
				throw new NoSuchRepoException(repoId);
			}

			final Repo loadedRepo = loadRepoData(db, repoRecord);

			this.repoCache.put(loadedRepo.getRepoId(), loadedRepo);

			return loadedRepo;
		}
	}

	/**
	 * @return Gets a list of all tracked repositories.
	 */
	public Collection<Repo> getAllRepos() {
		// Check cache
		List<Repo> repoList = new ArrayList<>(this.repoCache.asMap().values());

		// Check database
		List<String> cachedRepoIdList = repoList.stream()
			.map(repo -> repo.getRepoId().getId().toString())
			.collect(toList());

		try (DSLContext db = databaseStorage.acquireContext()) {
			db.fetch(REPOSITORY, REPOSITORY.ID.notIn(cachedRepoIdList))
				.stream()
				.map(record -> loadRepoData(db, record))
				.forEach(repoList::add);
		}

		return repoList;
	}

	/**
	 * @return Gets a list of the ids of all tracked repositories.
	 */
	public Collection<RepoId> getAllRepoIds() {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.fetch(REPOSITORY)
				.stream()
				.map(record -> new RepoId(UUID.fromString(record.getId())))
				.collect(toList());
		}
	}

	// --- Branch Getters -------------------------------------------------------------------------

	/**
	 * Gets a list of all tracked branches of the specified repository.
	 *
	 * @param repoId the id of the repository
	 * @return a list of tracked branches
	 */
	public Collection<Branch> getTrackedBranches(RepoId repoId) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.selectFrom(TRACKED_BRANCH)
				.where(TRACKED_BRANCH.REPO_ID.eq(repoId.getId().toString()))
				.fetch()
				.stream()
				.map(r -> BranchName.fromName(r.getBranchName()))
				.map(b -> new Branch(repoId, b))
				.collect(toSet());
		}
	}

	/**
	 * Gets a list of all branches of the specified repository.
	 *
	 * @param repoId the id of the repository
	 * @return a list of all branches
	 */
	public Collection<Branch> getBranches(RepoId repoId) {
		try (Repository localRepo = repoStorage.acquireRepository(repoId.getDirectoryName())) {
			List<Branch> nameList = new ArrayList<>();

			for (Ref branchRef : Git.wrap(localRepo).branchList().call()) {
				Branch branch = new Branch(repoId, BranchName.fromFullName(branchRef.getName()));
				nameList.add(branch);
			}

			return nameList;
		} catch (NoSuchRepositoryException | RepositoryAcquisitionException | GitAPIException e) {
			throw new RepoAccessException(repoId, e);
		}
	}

	// --- Latest Commit Hash Getters -------------------------------------------------------------

	/**
	 * Gets the commit hash that the given branch is pointing to.
	 *
	 * @param branch the branch
	 * @return the commit hash
	 */
	public CommitHash getLatestCommitHash(Branch branch) {
		return loadLatestCommitHash(
			branch.getRepoId().getDirectoryName(), branch.getName().getFullName()
		);
	}

	/**
	 * Gets the commit hash that the benchmark repository's HEAD is pointing to.
	 *
	 * @return the commit hash
	 */
	public CommitHash getLatestBenchmarkRepoHash() {
		return loadLatestCommitHash(benchRepoDirName, Constants.HEAD);
	}

	// --- Additional Getters ---------------------------------------------------------------------

	/**
	 * Gets the remote url of the specified repository
	 *
	 * @param repoId the id of the repository
	 * @return the remote url
	 * @throws NoSuchRepoException if no repository with that id exists
	 */
	public RemoteUrl getRemoteUrl(RepoId repoId) {
		// Check cache
		final Repo cachedRepo = this.repoCache.getIfPresent(repoId);
		if (cachedRepo != null) {
			return cachedRepo.getRemoteUrl();
		}

		// Check database
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.select(REPOSITORY.REMOTE_URL)
				.from(REPOSITORY)
				.where(REPOSITORY.ID.eq(repoId.getId().toString()))
				.fetchOptional()
				.map(Record1::value1)
				.map(RemoteUrl::new)
				.orElseThrow(() -> new NoSuchRepoException(repoId));
		}
	}

	// --- Additional Data Loading Methods --------------------------------------------------------

	private Repo loadRepoData(DSLContext db, RepositoryRecord repoRecord)
		throws RepoAccessException {

		RepoId repoId = new RepoId(UUID.fromString(repoRecord.getId()));

		// Load tracked branches from database
		Set<Branch> trackedBranches = db.selectFrom(TRACKED_BRANCH)
			.where(TRACKED_BRANCH.REPO_ID.eq(repoId.getId().toString()))
			.fetch()
			.stream()
			.map(r -> BranchName.fromName(r.getBranchName()))
			.map(b -> new Branch(repoId, b))
			.collect(toSet());

		return new Repo(
			repoId,
			repoRecord.getName(),
			new RemoteUrl(repoRecord.getRemoteUrl()),
			trackedBranches
		);
	}

	private CommitHash loadLatestCommitHash(String dirName, String ref) {
		try (Repository localRepo = repoStorage.acquireRepository(dirName)) {
			ObjectId refPtr = localRepo.resolve(ref);
			return new CommitHash(refPtr.getName()); // returns sha-1 hash
		} catch (NoSuchRepositoryException | RepositoryAcquisitionException | IOException e) {
			throw new RepoAccessException("failed to access ref " + ref + " under local repo "
				+ dirName, e);
		}
	}

}
