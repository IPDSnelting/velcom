package de.aaaaaaah.velcom.backend.newaccess.committaccess;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.tables.CommitRelationship.COMMIT_RELATIONSHIP;
import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;

import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.FullCommit;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.exceptions.NoSuchCommitException;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.codegen.db.tables.records.CommitRelationshipRecord;
import org.jooq.codegen.db.tables.records.KnownCommitRecord;
import org.jooq.exception.DataAccessException;

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
			KnownCommitRecord record = db.fetchOne(
				KNOWN_COMMIT,
				KNOWN_COMMIT.REPO_ID.eq(repoId.getIdAsString())
					.and(KNOWN_COMMIT.HASH.eq(commitHash.getHash()))
			);
			return knownCommitRecordToCommit(record);
		} catch (DataAccessException e) {
			throw new NoSuchCommitException(e, repoId, commitHash);
		}
	}

	public List<Commit> getCommits(RepoId repoId, Collection<CommitHash> commitHashes) {
		Set<String> hashes = commitHashes.stream()
			.map(CommitHash::getHash)
			.collect(toSet());

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectFrom(KNOWN_COMMIT)
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
			KnownCommitRecord record = db.fetchOne(
				KNOWN_COMMIT,
				KNOWN_COMMIT.REPO_ID.eq(repoId.getIdAsString())
					.and(KNOWN_COMMIT.HASH.eq(commitHash.getHash()))
			);

			Set<CommitHash> parentHashes = db.select(COMMIT_RELATIONSHIP.PARENT_HASH)
				.from(COMMIT_RELATIONSHIP)
				.where(COMMIT_RELATIONSHIP.REPO_ID.eq(repoId.getIdAsString()))
				.and(COMMIT_RELATIONSHIP.CHILD_HASH.eq(commitHash.getHash()))
				.stream()
				.map(Record1::value1)
				.map(CommitHash::new)
				.collect(toSet());

			Set<CommitHash> childHashes = db.select(COMMIT_RELATIONSHIP.CHILD_HASH)
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
			return db.select(COMMIT_RELATIONSHIP.PARENT_HASH)
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
			return db.select(COMMIT_RELATIONSHIP.CHILD_HASH)
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

				Map<String, Set<CommitHash>> allParentHashes = db.selectFrom(COMMIT_RELATIONSHIP)
					.where(COMMIT_RELATIONSHIP.REPO_ID.eq(entry.getKey().getIdAsString()))
					.and(COMMIT_RELATIONSHIP.CHILD_HASH.in(hashes))
					.stream()
					.collect(groupingBy(
						CommitRelationshipRecord::getChildHash,
						mapping(it -> new CommitHash(it.getParentHash()), toSet())
					));

				Map<String, Set<CommitHash>> allChildHashes = db.selectFrom(COMMIT_RELATIONSHIP)
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
	 * Gets all tracked commits whose committer date is between the given start and end time.
	 *
	 * @param repoId the id of the repo
	 * @param startTime the start committer time
	 * @param stopTime the stop committer time
	 * @return all tracked commits whose committer date is between the given stanrd and end time
	 */
	public List<Commit> getTrackedCommitsBetween(RepoId repoId, @Nullable Instant startTime,
		@Nullable Instant stopTime) {

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			SelectConditionStep<KnownCommitRecord> query = db.selectFrom(KNOWN_COMMIT)
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
			+ "  WHERE branch.repo_id = ?" // Binding #1
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
			Set<String> reachableHashes = db.dsl().fetch(queryStr, bindings.toArray()).stream()
				.map(record -> record.get(0, String.class))
				.collect(toSet());

			// Part 2: Fetching commits
			//
			// This part fetches the reachable commits we're after based on the hashes returned from the
			// previous query with a normal jOOQ query. This filtering isn't included in the above
			// recursive query because when we tried to do that, jdbc/jOOQ failed to correctly parse the
			// timestamps for the author/committer dates for some reason.

			SelectConditionStep<KnownCommitRecord> query = db.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getIdAsString()))
				.and(KNOWN_COMMIT.HASH.in(reachableHashes));

			if (startTime != null) {
				query = query.and(KNOWN_COMMIT.AUTHOR_DATE.ge(startTime));
			}
			if (stopTime != null) {
				query = query.and(KNOWN_COMMIT.AUTHOR_DATE.le(stopTime));
			}

			return query.stream()
				.map(CommitReadAccess::knownCommitRecordToCommit)
				.collect(toList());
		}
	}
}
