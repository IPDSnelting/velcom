package de.aaaaaaah.velcom.backend.newaccess;

import static de.aaaaaaah.velcom.backend.newaccess.entities.BenchmarkStatus.BENCHMARK_REQUIRED;
import static de.aaaaaaah.velcom.backend.newaccess.entities.BenchmarkStatus.BENCHMARK_REQUIRED_MANUAL_PRIORITY;
import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;

import de.aaaaaaah.velcom.backend.newaccess.entities.BenchmarkStatus;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.util.Pair;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.Record1;

/**
 * Provides read access to known commits and their respective statuses.
 */
public class KnownCommitReadAccess {

	final DatabaseStorage databaseStorage;

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

	public Set<Pair<RepoId, CommitHash>> getAllCommitsRequiringBenchmark() {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.select(KNOWN_COMMIT.REPO_ID, KNOWN_COMMIT.HASH)
				.from(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.STATUS.eq(BENCHMARK_REQUIRED.getNumericalValue()))
				.or(KNOWN_COMMIT.STATUS.eq(BENCHMARK_REQUIRED_MANUAL_PRIORITY.getNumericalValue()))
				.stream()
				.map(r -> new Pair<>(
					new RepoId(UUID.fromString(r.value1())),
					new CommitHash(r.value2())
				))
				.collect(Collectors.toUnmodifiableSet());
		}
	}

}
