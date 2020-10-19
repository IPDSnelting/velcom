package de.aaaaaaah.velcom.backend.listener;

import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import org.eclipse.jgit.lib.Repository;

/**
 * For a single repo, update the db to mirror the actual git repo's branches and commits.
 */
public class DbUpdater {

	private final Repo repo;
	private final Repository jgitRepo;
	private final DBWriteAccess db;

	public DbUpdater(Repo repo, Repository jgitRepo, DBWriteAccess db) {
		this.repo = repo;
		this.jgitRepo = jgitRepo;
		this.db = db;
	}

	public void updateBranches() {
		// TODO: 19.10.20 Implement
	}

	public void updateCommits() {
		// TODO: 19.10.20 Implement
	}
}
