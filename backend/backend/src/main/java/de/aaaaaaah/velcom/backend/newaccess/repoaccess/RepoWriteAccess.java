package de.aaaaaaah.velcom.backend.newaccess.repoaccess;

import static org.jooq.codegen.db.tables.Branch.BRANCH;
import static org.jooq.codegen.db.tables.Repo.REPO;
import static org.jooq.impl.DSL.field;

import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.exceptions.FailedToAddRepoException;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jooq.codegen.db.tables.records.RepoRecord;
import org.jooq.exception.DataAccessException;

/**
 * See {@link RepoReadAccess}.
 */
public class RepoWriteAccess extends RepoReadAccess {

	public RepoWriteAccess(DatabaseStorage databaseStorage) {
		super(databaseStorage);
	}

	public Repo addRepo(String name, RemoteUrl remoteUrl) throws FailedToAddRepoException {
		UUID id = UUID.randomUUID();
		RepoRecord record = new RepoRecord(id.toString(), name, remoteUrl.getUrl());

		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.insertInto(REPO)
				.values(record)
				.execute();
		} catch (DataAccessException e) {
			throw new FailedToAddRepoException(e, name, remoteUrl);
		}

		return new Repo(new RepoId(id), name, remoteUrl);
	}

	public void deleteRepo(RepoId repoId) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.deleteFrom(REPO)
				.where(REPO.ID.eq(repoId.getIdAsString()))
				.execute();
		}
	}

	public void updateRepo(RepoId repoId, @Nullable String name, @Nullable RemoteUrl remoteUrl) {
		// Not the nicest implementation, but simple to write and understand.

		if (name == null && remoteUrl == null) {
			return;
		}

		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			if (name != null && remoteUrl != null) {
				db.update(REPO)
					.set(REPO.NAME, name)
					.set(REPO.REMOTE_URL, remoteUrl.getUrl())
					.where(REPO.ID.eq(repoId.getIdAsString()))
					.execute();
			} else if (name != null) {
				db.update(REPO)
					.set(REPO.NAME, name)
					.where(REPO.ID.eq(repoId.getIdAsString()))
					.execute();
			} else {
				db.update(REPO)
					.set(REPO.REMOTE_URL, remoteUrl.getUrl())
					.where(REPO.ID.eq(repoId.getIdAsString()))
					.execute();
			}
		}
	}

	/**
	 * Sets which branches of this repo should be tracked.
	 *
	 * It may take a while for this change to take effect
	 * @param repoId
	 * @param trackedBranches
	 */
	public void setTrackedBranches(RepoId repoId, Collection<BranchName> trackedBranches) {
		Set<String> trackedNames = trackedBranches.stream()
			.map(BranchName::getName)
			.collect(Collectors.toSet());

		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.update(BRANCH)
				.set(BRANCH.TRACKED, field(BRANCH.NAME.in(trackedNames)))
				.where(BRANCH.REPO_ID.eq(repoId.getIdAsString()))
				.execute();
		}
	}
}
