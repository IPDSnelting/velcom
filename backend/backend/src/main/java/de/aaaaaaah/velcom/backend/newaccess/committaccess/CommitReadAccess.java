package de.aaaaaaah.velcom.backend.newaccess.committaccess;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.tables.CommitRelationship.COMMIT_RELATIONSHIP;
import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;

import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.FullCommit;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.exceptions.NoSuchCommitException;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jooq.Record1;
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
					.and(KNOWN_COMMIT.MIGRATED)
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
				.and(KNOWN_COMMIT.MIGRATED)
				.stream()
				.map(CommitReadAccess::knownCommitRecordToCommit)
				.collect(Collectors.toList());
		}
	}

	public FullCommit getFullCommit(RepoId repoId, CommitHash commitHash)
		throws NoSuchCommitException {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			KnownCommitRecord record = db.fetchOne(
				KNOWN_COMMIT,
				KNOWN_COMMIT.REPO_ID.eq(repoId.getIdAsString())
					.and(KNOWN_COMMIT.HASH.eq(commitHash.getHash()))
					.and(KNOWN_COMMIT.MIGRATED)
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

	public List<Commit> getCommitsBetween(RepoId repoId, Collection<BranchName> startBranches,
		Instant startTime, Instant stopTime) {

		// TODO: 19.10.20 Remove this dummy function
		return List.of();
	}

	public Optional<List<CommitHash>> getChildren(RepoId repoId, CommitHash commitHash,
		Collection<BranchName> startBranches) {

		// TODO: 19.10.20 Remove this dummy function
		return Optional.empty();
	}
}
