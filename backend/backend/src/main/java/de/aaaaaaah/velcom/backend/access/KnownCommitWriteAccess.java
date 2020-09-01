package de.aaaaaaah.velcom.backend.access;

import static org.jooq.codegen.db.Tables.KNOWN_COMMIT;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.Collection;

/**
 * Provides access to known commits and their respective statuses.
 */
public class KnownCommitWriteAccess extends KnownCommitReadAccess {

	private final TaskWriteAccess taskAccess;

	public KnownCommitWriteAccess(DatabaseStorage databaseStorage, TaskWriteAccess taskAccess) {
		super(databaseStorage);
		this.taskAccess = taskAccess;
	}

	public void markCommitsAsKnownAndInsertIntoQueue(RepoId repoId, Collection<CommitHash> commits,
		Collection<Task> tasks) {

		databaseStorage.acquireWriteTransaction(db -> {
			markCommitsAsKnown(repoId, commits, db);
			taskAccess.insertTasks(tasks, db);
		});
	}

	public void markCommitsAsKnown(RepoId repoId, Collection<CommitHash> commits) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			markCommitsAsKnown(repoId, commits, db);
		}
	}

	private void markCommitsAsKnown(RepoId repoId, Collection<CommitHash> commits, DBWriteAccess db) {
		var insert = db.insertInto(KNOWN_COMMIT).columns(KNOWN_COMMIT.REPO_ID, KNOWN_COMMIT.HASH);
		commits.forEach(c -> insert.values(repoId.getId().toString(), c.getHash()));
		insert.onDuplicateKeyIgnore().execute();
	}

}
