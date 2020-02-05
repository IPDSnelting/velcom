package de.aaaaaaah.velcom.backend.newaccess;

import static de.aaaaaaah.velcom.backend.access.commit.BenchmarkStatus.BENCHMARK_REQUIRED;
import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;

import de.aaaaaaah.velcom.backend.newaccess.entities.BenchmarkStatus;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.util.Pair;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep5;
import org.jooq.Record1;
import org.jooq.codegen.db.tables.records.KnownCommitRecord;

public class KnownCommitReadAccess {

	private final DatabaseStorage databaseStorage;

	public KnownCommitReadAccess(DatabaseStorage databaseStorage) {
		this.databaseStorage = databaseStorage;
	}

	public boolean isKnown(RepoId repoId, CommitHash commitHash) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.fetchExists(db.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getId().toString()))
				.and(KNOWN_COMMIT.HASH.eq(commitHash.getHash())));
		}
	}

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
				.orElse(BenchmarkStatus.BENCHMARK_REQUIRED);
		}
	}

	public void setBenchmarkStatus(RepoId repoId, CommitHash commitHash,
		BenchmarkStatus benchmarkStatus) {

		setBenchmarkStatus(repoId, List.of(commitHash), benchmarkStatus);
	}

	public void setBenchmarkStatus(RepoId repoId, Collection<CommitHash> commitHashes,
		BenchmarkStatus benchmarkStatus) {

		try (DSLContext db = databaseStorage.acquireContext()) {
			final InsertValuesStep5<KnownCommitRecord, String, String, Integer, Timestamp, Timestamp> step = db
				.insertInto(KNOWN_COMMIT, KNOWN_COMMIT.REPO_ID, KNOWN_COMMIT.HASH,
					KNOWN_COMMIT.STATUS, KNOWN_COMMIT.UPDATE_TIME, KNOWN_COMMIT.INSERT_TIME);

			int statusInteger = benchmarkStatus.getNumericalValue();
			Timestamp now = Timestamp.from(Instant.now());

			for (CommitHash hash : commitHashes) {
				step.values(repoId.getId().toString(), hash.getHash(), statusInteger, now, now);
			}

			step.onDuplicateKeyUpdate()
				.set(KNOWN_COMMIT.STATUS, statusInteger)
				.set(KNOWN_COMMIT.UPDATE_TIME, now)
				.execute();
		}
	}

	public Set<CommitHash> getAllCommitsOfStatus(RepoId repoId, BenchmarkStatus status) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getId().toString()))
				.and(KNOWN_COMMIT.STATUS.eq(status.getNumericalValue()))
				.stream()
				.map(KnownCommitRecord::getHash)
				.map(CommitHash::new)
				.collect(Collectors.toUnmodifiableSet());
		}
	}

	public Set<Pair<RepoId, CommitHash>> getAllCommitsOfStatus(BenchmarkStatus status) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.select(KNOWN_COMMIT.REPO_ID, KNOWN_COMMIT.HASH)
				.from(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.STATUS.eq(BENCHMARK_REQUIRED.getNumericalValue()))
				.or(KNOWN_COMMIT.STATUS.eq(status.getNumericalValue()))
				.stream()
				.map(r -> new Pair<>(
					new RepoId(UUID.fromString(r.value1())),
					new CommitHash(r.value2())
				))
				.collect(Collectors.toUnmodifiableSet());
		}
	}

	public Set<CommitHash> getKnownCommits(RepoId repoId, Collection<CommitHash> allHashes) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.select(KNOWN_COMMIT.HASH)
				.from(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getId().toString()))
				.and(KNOWN_COMMIT.HASH.in(allHashes))
				.stream()
				.map(Record1::value1)
				.map(CommitHash::new)
				.collect(Collectors.toUnmodifiableSet());
		}
	}
}
