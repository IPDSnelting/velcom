package de.aaaaaaah.velcom.backend.newaccess;

import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;

import de.aaaaaaah.velcom.backend.newaccess.entities.BenchmarkStatus;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep5;
import org.jooq.codegen.db.tables.records.KnownCommitRecord;

/**
 * Provides access to known commits and their respective statuses.
 */
public class KnownCommitWriteAccess extends KnownCommitReadAccess {

	public KnownCommitWriteAccess(DatabaseStorage databaseStorage) {
		super(databaseStorage);
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
}
