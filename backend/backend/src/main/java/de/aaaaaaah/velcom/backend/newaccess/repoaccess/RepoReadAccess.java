package de.aaaaaaah.velcom.backend.newaccess.repoaccess;

import static org.jooq.codegen.db.tables.Branch.BRANCH;
import static org.jooq.codegen.db.tables.Repo.REPO;

import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.codegen.db.tables.records.BranchRecord;
import org.jooq.codegen.db.tables.records.RepoRecord;
import org.jooq.exception.DataAccessException;

/**
 * Access for repos and their (tracked and untracked) branches.
 */
public class RepoReadAccess {

	protected final DatabaseStorage databaseStorage;

	public RepoReadAccess(DatabaseStorage databaseStorage) {
		this.databaseStorage = databaseStorage;
	}

	private static Repo repoRecordToRepo(RepoRecord record) {
		return new Repo(
			RepoId.fromString(record.getId()),
			record.getName(),
			new RemoteUrl(record.getRemoteUrl())
		);
	}

	private static Branch branchRecordToBranch(BranchRecord record) {
		return new Branch(
			RepoId.fromString(record.getRepoId()),
			BranchName.fromName(record.getName()),
			new CommitHash(record.getLatestCommitHash()),
			record.getTracked()
		);
	}

	public List<Repo> getAllRepos() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.fetch(REPO).stream()
				.map(RepoReadAccess::repoRecordToRepo)
				.collect(Collectors.toList());
		}
	}

	public Repo getRepo(RepoId repoId) throws NoSuchRepoException {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			RepoRecord repoRecord = db.fetchOne(REPO, REPO.ID.eq(repoId.getIdAsString()));
			return repoRecordToRepo(repoRecord);
		} catch (DataAccessException e) {
			throw new NoSuchRepoException(e, repoId);
		}
	}

	public void guardRepoExists(RepoId repoId) throws NoSuchRepoException {
		getRepo(repoId);
	}

	public List<Branch> getAllBranches(RepoId repoId) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.fetch(
				BRANCH,
				BRANCH.REPO_ID.eq(repoId.getIdAsString())
					.and(BRANCH.MIGRATED)
			)
				.stream()
				.map(RepoReadAccess::branchRecordToBranch)
				.collect(Collectors.toList());
		}
	}

	public List<Branch> getTrackedBranches(RepoId repoId) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.fetch(
				BRANCH,
				BRANCH.REPO_ID.eq(repoId.getIdAsString())
					.and(BRANCH.TRACKED)
					.and(BRANCH.MIGRATED)
			)
				.stream()
				.map(RepoReadAccess::branchRecordToBranch)
				.collect(Collectors.toList());
		}
	}

	public List<Branch> getUntrackedBranches(RepoId repoId) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.fetch(
				BRANCH,
				BRANCH.REPO_ID.eq(repoId.getIdAsString())
					.and(BRANCH.TRACKED.neg())
					.and(BRANCH.MIGRATED)
			)
				.stream()
				.map(RepoReadAccess::branchRecordToBranch)
				.collect(Collectors.toList());
		}
	}
}
