package de.aaaaaaah.velcom.backend.access.commit;

import static de.aaaaaaah.velcom.backend.access.commit.BenchmarkStatus.BENCHMARK_REQUIRED;
import static de.aaaaaaah.velcom.backend.access.commit.BenchmarkStatus.BENCHMARK_REQUIRED_MANUAL_PRIORITY;
import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;

import de.aaaaaaah.velcom.backend.access.AccessLayer;
import de.aaaaaaah.velcom.backend.access.repo.Branch;
import de.aaaaaaah.velcom.backend.access.repo.BranchName;
import de.aaaaaaah.velcom.backend.access.repo.Repo;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.codegen.db.tables.records.KnownCommitRecord;

/**
 * This class abstracts away access to commits and the commit history of repositories.
 */
public class CommitAccess {

	private final AccessLayer accessLayer;
	private final DatabaseStorage databaseStorage;
	private final RepoStorage repoStorage;

	/**
	 * This constructor also registers the {@link CommitAccess} in the accessLayer.
	 *
	 * @param accessLayer the {@link AccessLayer} to register with
	 * @param databaseStorage a database storage
	 * @param repoStorage a repo storage
	 */
	public CommitAccess(AccessLayer accessLayer, DatabaseStorage databaseStorage,
		RepoStorage repoStorage) {

		this.accessLayer = accessLayer;
		this.databaseStorage = databaseStorage;
		this.repoStorage = repoStorage;

		accessLayer.registerCommitAccess(this);
	}

	// Commit operations

	public Commit getCommit(RepoId repoId, CommitHash commitHash) throws CommitAccessException {
		try (Repository repo = repoStorage.acquireRepository(repoId.getDirectoryName())) {
			try (RevWalk walk = new RevWalk(repo)) {
				ObjectId commitPtr = repo.resolve(commitHash.getHash());
				RevCommit revCommit = walk.parseCommit(commitPtr);

				return commitFromRevCommit(repoId, revCommit);
			}
		} catch (RepositoryAcquisitionException | IOException e) {
			throw new CommitAccessException("Failed to access commit " + commitHash
				+ " in repository " + repoId, e);
		}
	}

	public Collection<Commit> getCommits(RepoId repoId, Collection<CommitHash> commitHashes)
		throws CommitAccessException {
		List<Commit> commitList = new ArrayList<>(commitHashes.size());

		try (Repository repo = repoStorage.acquireRepository(repoId.getDirectoryName())) {
			try (RevWalk walk = new RevWalk(repo)) {
				for (CommitHash commitHash : commitHashes) {
					ObjectId commitPtr = repo.resolve(commitHash.getHash());
					RevCommit revCommit = walk.parseCommit(commitPtr);

					Commit commit = commitFromRevCommit(repoId, revCommit);

					commitList.add(commit);
				}
			}
		} catch (RepositoryAcquisitionException | IOException e) {
			throw new CommitAccessException("Failed to access multiple commits in repository "
				+ repoId, e);
		}

		return commitList;
	}

	// Mutable properties

	/**
	 * See {@link Commit#isKnown()}. To make an unknown commit known, use {@link
	 * #setBenchmarkStatus(RepoId, CommitHash, BenchmarkStatus)} to set its benchmark status. To
	 * make a known commit unknown, use {@link #makeUnknown(RepoId, CommitHash)}.
	 *
	 * @param repoId the repo the commit is in
	 * @param hash the commit's hash
	 * @return whether the commit is known. Defaults to false if the repo id is invalid
	 */
	public boolean isKnown(RepoId repoId, CommitHash hash) {
		// Commit is known if resides in database
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.fetchExists(db.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getId().toString()))
				.and(KNOWN_COMMIT.HASH.eq(hash.getHash())));
		}
	}

	/**
	 * Remove a commit from the list of known commits. This also deletes associated information,
	 * such as the commit's benchmark status.
	 *
	 * @param repoId the repo the commit is in
	 * @param hash the commit's hash
	 */
	public void makeUnknown(RepoId repoId, CommitHash hash) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.deleteFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getId().toString()))
				.and(KNOWN_COMMIT.HASH.eq(hash.getHash()))
				.execute();
		}
	}

	/**
	 * Checks whether or not any of specified repository's commits are known at this point.
	 *
	 * @param repoId the id of the repository
	 * @return true if at least one commit of the repository is known
	 */
	public boolean hasKnownCommits(RepoId repoId) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.fetchExists(db.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getId().toString())));
		}
	}

	public BenchmarkStatus getBenchmarkStatus(RepoId repoId, CommitHash commitHash) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.select(KNOWN_COMMIT.STATUS).from(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getId().toString()))
				.and(KNOWN_COMMIT.HASH.eq(commitHash.getHash()))
				.fetchOptional()
				.map(Record1::value1)
				.map(BenchmarkStatus::fromNumericalValue)
				.orElse(BENCHMARK_REQUIRED);
		}
	}

	public void setBenchmarkStatus(RepoId repoId, CommitHash commitHash, BenchmarkStatus status) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.insertInto(KNOWN_COMMIT)
				.set(KNOWN_COMMIT.REPO_ID, repoId.getId().toString())
				.set(KNOWN_COMMIT.HASH, commitHash.getHash())
				.set(KNOWN_COMMIT.STATUS, status.getNumericalValue())
				.set(KNOWN_COMMIT.UPDATE_TIME, Timestamp.from(Instant.now()))
				.set(KNOWN_COMMIT.INSERT_TIME, Timestamp.from(Instant.now()))
				.onDuplicateKeyUpdate()
				.set(KNOWN_COMMIT.STATUS, status.getNumericalValue())
				.set(KNOWN_COMMIT.UPDATE_TIME, Timestamp.from(Instant.now()))
				.execute();
		}
	}

	public Collection<Commit> getAllTasksOfStatus(RepoId repoId, BenchmarkStatus status)
		throws CommitAccessException {

		try (Repository repo = repoStorage.acquireRepository(repoId.getDirectoryName())) {
			Result<KnownCommitRecord> results;

			try (DSLContext db = databaseStorage.acquireContext()) {
				results = db.selectFrom(KNOWN_COMMIT)
					.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getId().toString()))
					.and(KNOWN_COMMIT.STATUS.eq(status.getNumericalValue()))
					.fetch();
			}

			List<Commit> commitList = new ArrayList<>(results.size());

			try (RevWalk walk = new RevWalk(repo)) {
				for (KnownCommitRecord knownCommitRecord : results) {
					ObjectId commitPtr = repo.resolve(knownCommitRecord.getHash());
					RevCommit revCommit = walk.parseCommit(commitPtr);

					Commit commit = commitFromRevCommit(repoId, revCommit);

					commitList.add(commit);
				}
			}

			return commitList;
		} catch (RepositoryAcquisitionException | IOException e) {
			throw new CommitAccessException("Failed to get all tasks of status " + status
				+ " from repo " + repoId);
		}
	}

	// Advanced operations

	public Collection<Commit> getAllCommitsRequiringBenchmark() {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.STATUS.eq(BENCHMARK_REQUIRED.getNumericalValue()))
				.or(KNOWN_COMMIT.STATUS.eq(BENCHMARK_REQUIRED_MANUAL_PRIORITY.getNumericalValue()))
				.fetch()
				.map(record -> getCommit(
					new RepoId(UUID.fromString(record.getRepoId())),
					new CommitHash(record.getHash())
				));
		}
	}

	// TODO find out more about jgit's commit order
	// TODO What about the RepoStorage lock? Is the CommitAccessException enough?

	/**
	 * Constructs a new commit walk instance starting at the commit that the given branch points
	 * at.
	 *
	 * @param branch the branch whose commit it points to should be the start
	 * @return returns the commit walk instance
	 */
	public CommitWalk getCommitWalk(Branch branch) {
		return getCommitWalk(branch.getCommit());
	}

	/**
	 * Constructs a new commit walk instance starting at the specified commit.
	 *
	 * @param startCommit the commit to start at
	 * @return the commit walk instance
	 */
	public CommitWalk getCommitWalk(Commit startCommit) {
		RepoId repoId = startCommit.getRepoId();
		Repository repo = null;
		RevWalk walk = null;

		try {
			repo = repoStorage.acquireRepository(repoId.getDirectoryName());
			walk = new RevWalk(repo);

			return new CommitWalk(this, repoId, repo, walk, startCommit);
		} catch (Exception e) {
			if (walk != null) {
				walk.close();
			}
			if (repo != null) {
				repo.close();
			}
			throw new CommitAccessException("Failed to create commit walk for: " + startCommit, e);
		}
	}

	/**
	 * Returns all commits in the specified branches in the order that jgit puts them in. The stream
	 * must be closed manually once it is no longer required.
	 *
	 * @param repo the repo to take the commits from
	 * @param branches the branches to take the commits from
	 * @return the commits
	 * @throws NoSuchCommitException if anything goes wrong in the underlying jgit commit
	 * 	traversal
	 */
	public Stream<Commit> getCommitLog(Repo repo, Collection<BranchName> branches)
		throws CommitAccessException {

		// Step 1: Acquire repository
		Repository jgitRepo;

		try {
			String directoryName = repo.getId().getDirectoryName();
			jgitRepo = repoStorage.acquireRepository(directoryName);
		} catch (RepositoryAcquisitionException e) {
			throw new CommitAccessException(e);
		}

		try {
			// Step 2: Run log command
			LogCommand logCommand = Git.wrap(jgitRepo).log();

			for (BranchName branchName : branches) {

				ObjectId branchId = jgitRepo.resolve(branchName.getName());

				logCommand.add(branchId);
			}

			// Step 3: Prepare stream
			Spliterator<RevCommit> commitSpliterator = Spliterators.spliteratorUnknownSize(
				logCommand.call().iterator(), 0);

			return StreamSupport.stream(commitSpliterator, false)
				.map(revCommit -> commitFromRevCommit(repo.getId(), revCommit))
				.onClose(jgitRepo::close);
		} catch (IOException | GitAPIException e) {
			jgitRepo.close(); // Release repo storage lock if this fails
			throw new CommitAccessException(e);
		}
	}

	Commit commitFromRevCommit(RepoId repoId, RevCommit revCommit) {
		CommitHash ownHash = new CommitHash(revCommit.getId().getName());
		List<CommitHash> parentHashes = List.of(revCommit.getParents()).stream()
			.map(RevCommit::getId)
			.map(AnyObjectId::getName)
			.map(CommitHash::new)
			.collect(Collectors.toUnmodifiableList());

		PersonIdent author = revCommit.getAuthorIdent();
		PersonIdent committer = revCommit.getCommitterIdent();

		return new Commit(
			accessLayer.getCommitAccess(),
			accessLayer.getRepoAccess(),
			repoId,
			ownHash,
			parentHashes,
			author.toExternalString(),
			author.getWhen().toInstant(),
			committer.toExternalString(),
			committer.getWhen().toInstant(),
			revCommit.getFullMessage()
		);
	}

	public Collection<Commit> getCommitsBetween(RepoId repoId, Collection<String> branches,
		@Nullable Instant startTime, @Nullable Instant stopTime) {

		// TODO implement
		return null;
	}
}
