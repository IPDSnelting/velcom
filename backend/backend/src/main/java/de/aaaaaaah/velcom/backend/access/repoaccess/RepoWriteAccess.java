package de.aaaaaaah.velcom.backend.access.repoaccess;

import static org.jooq.codegen.db.tables.Branch.BRANCH;
import static org.jooq.codegen.db.tables.GithubCommand.GITHUB_COMMAND;
import static org.jooq.codegen.db.tables.Repo.REPO;
import static org.jooq.impl.DSL.field;

import de.aaaaaaah.velcom.backend.access.caches.AvailableDimensionsCache;
import de.aaaaaaah.velcom.backend.access.caches.LatestRunCache;
import de.aaaaaaah.velcom.backend.access.caches.RunCache;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.repoaccess.exceptions.FailedToAddRepoException;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.time.Instant;
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

	private final AvailableDimensionsCache availableDimensionsCache;
	private final RunCache runCache;
	private final LatestRunCache latestRunCache;

	public RepoWriteAccess(DatabaseStorage databaseStorage,
		AvailableDimensionsCache availableDimensionsCache, RunCache runCache,
		LatestRunCache latestRunCache) {

		super(databaseStorage);

		this.availableDimensionsCache = availableDimensionsCache;
		this.runCache = runCache;
		this.latestRunCache = latestRunCache;
	}

	/**
	 * Add a new repo to the database. Does not clone the repo.
	 *
	 * @param name the repo's name
	 * @param remoteUrl the repo's remote url
	 * @return the newly added repo
	 * @throws FailedToAddRepoException if the repo could not be added to the database
	 */
	public Repo addRepo(String name, RemoteUrl remoteUrl) throws FailedToAddRepoException {
		UUID id = UUID.randomUUID();
		RepoRecord record = new RepoRecord(id.toString(), name, remoteUrl.getUrl(), null, null);

		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.dsl().batchInsert(record).execute();
		} catch (DataAccessException e) {
			throw new FailedToAddRepoException(e, name, remoteUrl);
		}

		return new Repo(new RepoId(id), name, remoteUrl, null, null);
	}

	/**
	 * Delete an existing repo from the database. Does nothing if the repo is not in the database.
	 *
	 * @param repoId the repo's id
	 */
	public void deleteRepo(RepoId repoId) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.dsl()
				.deleteFrom(REPO)
				.where(REPO.ID.eq(repoId.getIdAsString()))
				.execute();
		}

		availableDimensionsCache.invalidate(repoId);

		// No need to be more efficient about it since this doesn't happen very often anyways.
		runCache.invalidateAll();

		latestRunCache.invalidate(repoId);
	}

	/**
	 * Change the name and/or remote url of a repo. Does nothing if no new name and no new remote url
	 * are given.
	 *
	 * @param repoId the repo's id
	 * @param name the new name (or null to keep the old name)
	 * @param remoteUrl the new remote url (or null to keep the old remote url)
	 */
	public void updateRepo(RepoId repoId, @Nullable String name, @Nullable RemoteUrl remoteUrl) {
		// Not the nicest implementation, but simple to write and understand.

		if (name == null && remoteUrl == null) {
			return;
		}

		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			if (name != null && remoteUrl != null) {
				db.dsl()
					.update(REPO)
					.set(REPO.NAME, name)
					.set(REPO.REMOTE_URL, remoteUrl.getUrl())
					.where(REPO.ID.eq(repoId.getIdAsString()))
					.execute();
			} else if (name != null) {
				db.dsl()
					.update(REPO)
					.set(REPO.NAME, name)
					.where(REPO.ID.eq(repoId.getIdAsString()))
					.execute();
			} else {
				db.dsl()
					.update(REPO)
					.set(REPO.REMOTE_URL, remoteUrl.getUrl())
					.where(REPO.ID.eq(repoId.getIdAsString()))
					.execute();
			}
		}
	}

	/**
	 * Sets which branches of this repo should be tracked. It may take a while for this change to take
	 * effect.
	 *
	 * @param repoId the id of the repo whose branches to set
	 * @param trackedBranches the new set of tracked branches
	 */
	public void setTrackedBranches(RepoId repoId, Collection<BranchName> trackedBranches) {
		Set<String> trackedNames = trackedBranches.stream()
			.map(BranchName::getName)
			.collect(Collectors.toSet());

		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.dsl()
				.update(BRANCH)
				.set(BRANCH.TRACKED, field(BRANCH.NAME.in(trackedNames)))
				.where(BRANCH.REPO_ID.eq(repoId.getIdAsString()))
				.execute();
		}
	}

	/**
	 * Enable Github bot functionality by setting a GitHub auth token.
	 */
	public void setGithubAuthToken(RepoId repoId, String token) {
		databaseStorage.acquireWriteTransaction(db -> {
			db.dsl()
				.update(REPO)
				.set(REPO.GITHUB_AUTH_TOKEN, token)
				.set(REPO.GITHUB_COMMENT_CUTOFF, Instant.now())
				.where(REPO.ID.eq(repoId.getIdAsString()))
				.execute();
		});
	}

	/**
	 * Disable Github bot functionality by removing the GitHub auth token.
	 */
	public void unsetGithubAuthToken(RepoId repoId) {
		databaseStorage.acquireWriteTransaction(db -> {
			db.dsl()
				.update(REPO)
				.set(REPO.GITHUB_AUTH_TOKEN, (String) null)
				.set(REPO.GITHUB_COMMENT_CUTOFF, (Instant) null)
				.where(REPO.ID.eq(repoId.getIdAsString()))
				.execute();

			db.dsl()
				.deleteFrom(GITHUB_COMMAND)
				.where(GITHUB_COMMAND.REPO_ID.eq(repoId.getIdAsString()))
				.execute();
		});
	}
}
