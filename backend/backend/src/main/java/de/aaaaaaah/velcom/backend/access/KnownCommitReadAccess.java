package de.aaaaaaah.velcom.backend.access;

import static org.jooq.codegen.db.tables.KnownCommit.KNOWN_COMMIT;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;

/**
 * Provides read access to known commits and their respective statuses.
 */
public class KnownCommitReadAccess {

	final DatabaseStorage databaseStorage;

	public KnownCommitReadAccess(DatabaseStorage databaseStorage) {
		this.databaseStorage = databaseStorage;
	}

	public boolean isKnown(RepoId repoId, CommitHash commitHash) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.fetchExists(db.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getId().toString()))
				.and(KNOWN_COMMIT.HASH.eq(commitHash.getHash())));
		}
	}

	public boolean hasKnownCommits(RepoId repoId) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.fetchExists(db.selectFrom(KNOWN_COMMIT)
				.where(KNOWN_COMMIT.REPO_ID.eq(repoId.getId().toString())));
		}
	}

}
