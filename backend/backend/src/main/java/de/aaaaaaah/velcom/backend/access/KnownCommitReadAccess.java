package de.aaaaaaah.velcom.backend.access;

import static de.aaaaaaah.velcom.backend.access.entities.BenchmarkStatus.BENCHMARK_REQUIRED;
import static de.aaaaaaah.velcom.backend.access.entities.BenchmarkStatus.BENCHMARK_REQUIRED_MANUAL_PRIORITY;
import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;

import de.aaaaaaah.velcom.backend.access.entities.BenchmarkStatus;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
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

}
