package de.aaaaaaah.velcom.backend.access.committaccess;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.tables.CommitRelationship.COMMIT_RELATIONSHIP;
import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;
import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.one;
import static org.jooq.impl.DSL.select;

import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.FullCommit;
import de.aaaaaaah.velcom.backend.access.committaccess.exceptions.NoSuchCommitException;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.exceptions.NoSuchDimensionException;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.shared.util.Pair;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.codegen.db.tables.records.CommitRelationshipRecord;
import org.jooq.codegen.db.tables.records.KnownCommitRecord;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.NoDataFoundException;
import org.jooq.exception.TooManyRowsException;

/**
 * Provides read access to the git commits stored in the database.
 */
public class CommitReadAccess {

	protected final DatabaseStorage databaseStorage;

	public CommitReadAccess(DatabaseStorage databaseStorage) {
		this.databaseStorage = databaseStorage;
	}

	private static Commit knownCommitRecordToCommit(KnownCommitRecord record) {
		return new Commit(
			RepoId.fromString(record.getRepoId()),
			new CommitHash(record.getHash()),
			record.getReachable(),
			record.getTracked(),
			record.getAuthor(),
			record.getAuthorDate(),
			record.getCommitter(),
			record.getCommitterDate(),
			record.getMessage()
		);
	}

	private static FullCommit knownCommitRecordToFullCommit(KnownCommitRecord record,
		Set<CommitHash> parentHashes, Set<CommitHash> childHashes) {

		return new FullCommit(
			RepoId.fromString(record.getRepoId()),
			new CommitHash(record.getHash()),
			record.getReachable(),
			record.getTracked(),
			record.getAuthor(),
			record.getAuthorDate(),
			record.getCommitter(),
			record.getCommitterDate(),
			record.getMessage(),
			parentHashes,
			childHashes
		);
	}

	private static FullCommit commitToFullCommit(Commit commit, Set<CommitHash> parentHashes,
		Set<CommitHash> childHashes) {

		return new FullCommit(
			commit.getRepoId(),
			commit.getHash(),
			commit.isReachable(),
			commit.isTracked(),
			commit.getAuthor(),
			commit.getAuthorDate(),
			commit.getCommitter(),
			commit.getCommitterDate(),
			commit.getMessage(),
			parentHashes,
			childHashes
		);
	}

	public Commit getCommit(RepoId repoId, CommitHash commitHash) throws NoSuchCommitException {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			KnownCommitRecord record = db.dsl()
				.fetchSingle(
					KNOWN_COMMIT,
					KNOWN_COMMIT.REPO_ID.eq(repoId.getIdAsString())
						.and(KNOWN_COMMIT.HASH.eq(commitHash.getHash()))
				);
			return knownCommitRecordToCommit(record);
		} catch (DataAccessException e) {
			throw new NoSuchCommitException(e, repoId, commitHash);
		}
	}

	/**
	 * Check if a commit exists.
	 *
	 * @param repoId the commit's repo id
	 * @param commitHash the commit's hash
	 * @throws NoSuchDimensionException if the commit doesn't exist
	 */
	public void guardCommitExists(RepoId repoId, CommitHash commitHash)
		throws NoSuchDimensionException {

		getCommit(repoId, commitHash);
	}

	/**
	 * Get multiple commits from a single repo at the same time.
	 *
	 * @param repoId the repo the commits ar in
	 * @param commitHashes the commits' hashes
	 * @return all commits which could be found. Commits could not be found are simply not included in
	 * 	this list.
	 */
	public List<Commit> getCommits(RepoId repoId, Collection<CommitHash> commitHashes) {
		Set<String> hashes = commitHashes.stream()
			.map(CommitHash::getHash)
			.collect(toSet());

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getIdAsString()))
				.and(KNOWN_COMMIT.HASH.in(hashes))
				.stream()
				.map(CommitReadAccess::knownCommitRecordToCommit)
				.collect(toList());
		}
	}

	public FullCommit getFullCommit(RepoId repoId, CommitHash commitHash)
		throws NoSuchCommitException {

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			KnownCommitRecord record = db.dsl()
				.fetchSingle(
					KNOWN_COMMIT,
					KNOWN_COMMIT.REPO_ID.eq(repoId.getIdAsString())
						.and(KNOWN_COMMIT.HASH.eq(commitHash.getHash()))
				);

			Set<CommitHash> parentHashes = db.dsl()
				.select(COMMIT_RELATIONSHIP.PARENT_HASH)
				.from(COMMIT_RELATIONSHIP)
				.where(COMMIT_RELATIONSHIP.REPO_ID.eq(repoId.getIdAsString()))
				.and(COMMIT_RELATIONSHIP.CHILD_HASH.eq(commitHash.getHash()))
				.stream()
				.map(Record1::value1)
				.map(CommitHash::new)
				.collect(toSet());

			Set<CommitHash> childHashes = db.dsl()
				.select(COMMIT_RELATIONSHIP.CHILD_HASH)
				.from(COMMIT_RELATIONSHIP)
				.where(COMMIT_RELATIONSHIP.REPO_ID.eq(repoId.getIdAsString()))
				.and(COMMIT_RELATIONSHIP.PARENT_HASH.eq(commitHash.getHash()))
				.stream()
				.map(Record1::value1)
				.map(CommitHash::new)
				.collect(toSet());

			return knownCommitRecordToFullCommit(record, parentHashes, childHashes);
		} catch (DataAccessException e) {
			throw new NoSuchCommitException(e, repoId, commitHash);
		}
	}

	public Set<CommitHash> getParentHashes(RepoId repoId, CommitHash commitHash) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.select(COMMIT_RELATIONSHIP.PARENT_HASH)
				.from(COMMIT_RELATIONSHIP)
				.where(COMMIT_RELATIONSHIP.REPO_ID.eq(repoId.getIdAsString()))
				.and(COMMIT_RELATIONSHIP.CHILD_HASH.eq(commitHash.getHash()))
				.stream()
				.map(Record1::value1)
				.map(CommitHash::new)
				.collect(toSet());
		}
	}

	public Set<CommitHash> getChildHashes(RepoId repoId, CommitHash commitHash) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.select(COMMIT_RELATIONSHIP.CHILD_HASH)
				.from(COMMIT_RELATIONSHIP)
				.where(COMMIT_RELATIONSHIP.REPO_ID.eq(repoId.getIdAsString()))
				.and(COMMIT_RELATIONSHIP.PARENT_HASH.eq(commitHash.getHash()))
				.stream()
				.map(Record1::value1)
				.map(CommitHash::new)
				.collect(toSet());
		}
	}

	/**
	 * Convert a list of {@link Commit}s to {@link FullCommit}s. If a commit's parents or children
	 * could not be found, they default to the empty set.
	 *
	 * @param commits the commits to promote
	 * @return the promoted commits
	 */
	public List<FullCommit> promoteCommits(List<Commit> commits) {
		Map<RepoId, Set<Commit>> commitsByRepo = commits.stream()
			.collect(groupingBy(Commit::getRepoId, toSet()));

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			List<FullCommit> result = new ArrayList<>();

			for (Entry<RepoId, Set<Commit>> entry : commitsByRepo.entrySet()) {
				Set<String> hashes = entry.getValue().stream()
					.map(Commit::getHash)
					.map(CommitHash::getHash)
					.collect(toSet());

				Map<String, Set<CommitHash>> allParentHashes = db.dsl()
					.selectFrom(COMMIT_RELATIONSHIP)
					.where(COMMIT_RELATIONSHIP.REPO_ID.eq(entry.getKey().getIdAsString()))
					.and(COMMIT_RELATIONSHIP.CHILD_HASH.in(hashes))
					.stream()
					.collect(groupingBy(
						CommitRelationshipRecord::getChildHash,
						mapping(it -> new CommitHash(it.getParentHash()), toSet())
					));

				Map<String, Set<CommitHash>> allChildHashes = db.dsl()
					.selectFrom(COMMIT_RELATIONSHIP)
					.where(COMMIT_RELATIONSHIP.REPO_ID.eq(entry.getKey().getIdAsString()))
					.and(COMMIT_RELATIONSHIP.PARENT_HASH.in(hashes))
					.stream()
					.collect(groupingBy(
						CommitRelationshipRecord::getParentHash,
						mapping(it -> new CommitHash(it.getChildHash()), toSet())
					));

				entry.getValue().stream()
					.map(commit -> commitToFullCommit(
						commit,
						allParentHashes.getOrDefault(commit.getHashAsString(), Set.of()),
						allChildHashes.getOrDefault(commit.getHashAsString(), Set.of())
					))
					.forEach(result::add);
			}

			return result;
		}
	}

	/**
	 * Gets all tracked commits whose <em>committer</em> date is between the given start and end
	 * time.
	 *
	 * @param repoId the id of the repo
	 * @param startTime the start committer time
	 * @param stopTime the stop committer time
	 * @return all tracked commits whose committer date is between the given start and end time in no
	 * 	particular order
	 */
	public List<Commit> getTrackedCommitsBetween(RepoId repoId, @Nullable Instant startTime,
		@Nullable Instant stopTime) {

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			SelectConditionStep<KnownCommitRecord> query = db.dsl()
				.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getIdAsString()))
				.and(KNOWN_COMMIT.TRACKED);

			if (startTime != null) {
				query = query.and(KNOWN_COMMIT.COMMITTER_DATE.ge(startTime));
			}
			if (stopTime != null) {
				query = query.and(KNOWN_COMMIT.COMMITTER_DATE.le(stopTime));
			}

			return query.stream()
				.map(CommitReadAccess::knownCommitRecordToCommit)
				.collect(toList());
		}
	}

	/**
	 * @param repoId the id of the root commit's repo
	 * @param rootHash the hash of the root commit
	 * @return all tracked commits descending from the given root.
	 */
	public List<CommitHash> getDescendantCommits(RepoId repoId, CommitHash rootHash) {
		String query = "WITH RECURSIVE\n"
			+ "\n"
			+ "initial(hash) AS (\n"
			+ "  VALUES (?)\n" // <-- Binding #1 - Root hash
			+ "),\n"
			+ "\n"
			+ "rec(hash) AS (\n"
			+ "  SELECT initial.hash\n"
			+ "  FROM initial\n"
			+ "  JOIN known_commit\n"
			+ "    ON initial.hash = known_commit.hash\n"
			+ "  WHERE known_commit.repo_id = ?\n" // <-- Binding #2 - repo id
			+ "  \n"
			+ "  UNION\n"
			+ "  \n"
			+ "  SELECT commit_relationship.child_hash\n"
			+ "  FROM commit_relationship\n"
			+ "  JOIN rec\n"
			+ "    ON rec.hash = commit_relationship.parent_hash\n"
			+ "  WHERE repo_id = ?\n" // <-- Binding #3 - repo id
			+ ")\n"
			+ "\n"
			+ "SELECT rec.hash \n"
			+ "FROM rec\n"
			+ "JOIN known_commit\n"
			+ "  ON rec.hash = known_commit.hash\n"
			+ "WHERE known_commit.tracked\n"
			+ "";

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.fetchLazy(query, rootHash.getHash(), repoId.getIdAsString(), repoId.getIdAsString())
				.stream()
				.map(record -> (String) record.getValue(0))
				.map(CommitHash::new)
				.collect(toList());
		}
	}

	/**
	 * @param repoId the id of the root commit's repo
	 * @param rootHash the hash of the root commit
	 * @return all tracked commits descending from the given root.
	 */
	public Optional<CommitHash> getFirstParentOfBranch(RepoId repoId, BranchName branch,
		CommitHash rootHash) {
		String query = "WITH RECURSIVE\n"
			// Commits reachable from the branch
			+ "reachable(hash) AS (\n"
			+ "  SELECT branch.latest_commit_hash\n"
			+ "  FROM branch\n"
			+ "  WHERE branch.repo_id = ?\n" // <-- Binding #1 - repo id
			+ "  AND branch.name = ?\n" // <-- Binding #2 - branch name
			+ "  \n"
			+ "  UNION\n"
			+ "  \n"
			+ "  SELECT commit_relationship.parent_hash\n"
			+ "  FROM commit_relationship\n"
			+ "  JOIN reachable\n"
			+ "    ON reachable.hash = commit_relationship.child_hash\n"
			+ "  WHERE commit_relationship.repo_id = ?\n" // <-- Binding #3 - repo id
			+ "),\n"
			+ "\n"
			// The starting commit
			+ "initial(hash) AS (\n"
			+ "  VALUES (?)\n" // <-- Binding #4 - hash of starting commit
			+ "),\n"
			+ "\n"
			// Parents of the starting commit that are not reachable from the branch
			+ "parents(hash) AS (\n"
			+ "  SELECT initial.hash\n"
			+ "  FROM initial\n"
			+ "  JOIN known_commit\n"
			+ "    ON initial.hash = known_commit.hash\n"
			+ "  WHERE known_commit.repo_id = ?\n" // <-- Binding #5 - repo id
			+ "  AND initial.hash NOT IN reachable\n"
			+ "  \n"
			+ "  UNION\n"
			+ "  \n"
			+ "  SELECT commit_relationship.parent_hash\n"
			+ "  FROM commit_relationship\n"
			+ "  JOIN parents\n"
			+ "    ON parents.hash = commit_relationship.child_hash\n"
			+ "  WHERE commit_relationship.repo_id = ?\n" // <-- Binding #6 - repo id
			+ "  AND commit_relationship.parent_hash NOT IN reachable\n"
			+ ")\n"
			+ "\n"
			// Choose the only parents of our 'parents' table that is reachable from the branch. If there
			// are none or multiple such parents, don't try to choose one and instead return nothing.
			+ "SELECT commit_relationship.parent_hash\n"
			+ "FROM commit_relationship\n"
			+ "  JOIN parents\n"
			+ "    ON parents.hash = commit_relationship.child_hash\n"
			+ "  JOIN reachable\n"
			+ "    ON reachable.hash = commit_relationship.parent_hash\n"
			+ "";

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			try {
				String hash = db.dsl()
					.fetchSingle(
						query,
						repoId.getIdAsString(),
						branch.getName(),
						repoId.getIdAsString(),
						rootHash.getHash(),
						repoId.getIdAsString(),
						repoId.getIdAsString()
					)
					.get(0, String.class);
				return Optional.of(new CommitHash(hash));
			} catch (NoDataFoundException | TooManyRowsException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * Gets all commits - tracked and untracked - whose <em>committer</em> date is between the given
	 * start and end time.
	 *
	 * @param repoId the id of the repo
	 * @param branchNames the names of all branches to search for commits
	 * @param startTime the start author time
	 * @param stopTime the stop author time
	 * @return all commits whose author date is between the given start and end time in no particular
	 * 	order
	 */
	public List<Commit> getCommitsBetween(RepoId repoId, Collection<BranchName> branchNames,
		@Nullable Instant startTime, @Nullable Instant stopTime) {

		// Kinda ugly but it works and is also relatively fast, so I'm fine with it.

		// Part 1: Recursive query
		//
		// This generates a recursive query with variable number of bindings as a string and then
		// executes it. This is, of course, pretty ugly but I can't get jOOQ to generate correct
		// recursive queries in the sqlite dialect.

		List<String> branchNamesStr = branchNames.stream()
			.map(BranchName::getName)
			.collect(toList());

		String branchNamesBindings = branchNames.stream()
			.map(s -> "?")
			.collect(Collectors.joining(", "));

		String queryStr = ""
			+ "WITH RECURSIVE rec(hash) AS (\n"
			+ "  SELECT branch.latest_commit_hash"
			+ "  FROM branch"
			+ "  WHERE branch.repo_id = ?" // <-- Binding #1
			+ "  AND branch.name IN (" + branchNamesBindings + ")\n" // <-- Binding #2 (multiple values)
			+ "  \n"
			+ "  UNION\n"
			+ "  \n"
			+ "  SELECT commit_relationship.parent_hash\n"
			+ "  FROM commit_relationship\n"
			+ "  JOIN rec\n"
			+ "    ON rec.hash = commit_relationship.child_hash\n"
			+ "  WHERE commit_relationship.repo_id = ?\n" // <-- Binding #3
			+ ")\n"
			+ "\n"
			+ "SELECT rec.hash\n"
			+ "FROM rec\n"
			+ "";
		List<Object> bindings = new ArrayList<>();
		bindings.add(repoId.getIdAsString());
		bindings.addAll(branchNamesStr);
		bindings.add(repoId.getIdAsString());

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			Set<String> reachableHashes = db.dsl()
				.fetch(queryStr, bindings.toArray())
				.stream()
				.map(record -> record.get(0, String.class))
				.collect(toSet());

			// Part 2: Fetching commits
			//
			// This part fetches the reachable commits we're after based on the hashes returned from the
			// previous query with a normal jOOQ query. This filtering isn't included in the above
			// recursive query because when we tried to do that, jdbc/jOOQ failed to correctly parse the
			// timestamps for the author/committer dates for some reason.

			SelectConditionStep<KnownCommitRecord> query = db.dsl()
				.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getIdAsString()))
				.and(KNOWN_COMMIT.HASH.in(reachableHashes));

			if (startTime != null) {
				query = query.and(KNOWN_COMMIT.COMMITTER_DATE.ge(startTime));
			}
			if (stopTime != null) {
				query = query.and(KNOWN_COMMIT.COMMITTER_DATE.le(stopTime));
			}

			return query.stream()
				.map(CommitReadAccess::knownCommitRecordToCommit)
				.collect(toList());
		}
	}

	// TODO: 06.06.21 Add tests for this function
	public List<Pair<Commit, Boolean>> searchCommits(int limit, @Nullable RepoId repoId,
		String queryStr) {

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			var query = db.dsl()
				.select(
					KNOWN_COMMIT.REPO_ID,
					KNOWN_COMMIT.HASH,
					KNOWN_COMMIT.REACHABLE,
					KNOWN_COMMIT.TRACKED,
					KNOWN_COMMIT.AUTHOR,
					KNOWN_COMMIT.AUTHOR_DATE,
					KNOWN_COMMIT.COMMITTER,
					KNOWN_COMMIT.COMMITTER_DATE,
					KNOWN_COMMIT.MESSAGE,
					field(exists(select(one())
						.from(RUN)
						.where(RUN.REPO_ID.eq(KNOWN_COMMIT.REPO_ID))
						.and(RUN.COMMIT_HASH.eq(KNOWN_COMMIT.HASH))
					))
				)
				.from(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.HASH.contains(queryStr)
					.or(KNOWN_COMMIT.MESSAGE.contains(queryStr))
					.or(KNOWN_COMMIT.AUTHOR.contains(queryStr))
					.or(KNOWN_COMMIT.COMMITTER.contains(queryStr)));

			if (repoId != null) {
				query = query.and(KNOWN_COMMIT.REPO_ID.eq(repoId.getIdAsString()));
			}

			return query.orderBy(KNOWN_COMMIT.COMMITTER_DATE.desc())
				.limit(limit)
				.stream()
				.map(record -> new Pair<>(
					new Commit(
						RepoId.fromString(record.value1()),
						new CommitHash(record.value2()),
						record.value3(),
						record.value4(),
						record.value5(),
						record.value6(),
						record.value7(),
						record.value8(),
						record.value9()
					),
					record.value10()
				))
				.collect(toList());
		}
	}
}
