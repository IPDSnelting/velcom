package de.aaaaaaah.velcom.backend.access.repoaccess;

import static java.util.stream.Collectors.toList;
import static org.jooq.codegen.db.tables.Branch.BRANCH;
import static org.jooq.codegen.db.tables.GithubCommand.GITHUB_COMMAND;
import static org.jooq.codegen.db.tables.Repo.REPO;

import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Branch;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.repoaccess.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.listener.github.GithubCommand;
import de.aaaaaaah.velcom.backend.listener.github.GithubCommandState;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.List;
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
			new RemoteUrl(record.getRemoteUrl()),
			record.getGithubAuthToken(),
			record.getGithubCommentCutoff()
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
			return db.dsl()
				.fetch(REPO).stream()
				.map(RepoReadAccess::repoRecordToRepo)
				.collect(toList());
		}
	}

	public Repo getRepo(RepoId repoId) throws NoSuchRepoException {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			RepoRecord repoRecord = db.dsl().fetchSingle(REPO, REPO.ID.eq(repoId.getIdAsString()));
			return repoRecordToRepo(repoRecord);
		} catch (DataAccessException e) {
			throw new NoSuchRepoException(e, repoId);
		}
	}

	/**
	 * Check if a repo exists.
	 *
	 * @param repoId the id of the repo to look for
	 * @throws NoSuchRepoException if the repo doesn't exist
	 */
	public void guardRepoExists(RepoId repoId) throws NoSuchRepoException {
		getRepo(repoId);
	}

	/**
	 * Retrieve all of a repo's branches.
	 *
	 * @param repoId the id of the repo whose branches to return
	 * @return all of the repo's branches, or an empty list if the repo doesn't exist
	 */
	public List<Branch> getAllBranches(RepoId repoId) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.fetch(
					BRANCH,
					BRANCH.REPO_ID.eq(repoId.getIdAsString())
				)
				.stream()
				.map(RepoReadAccess::branchRecordToBranch)
				.collect(toList());
		}
	}

	/**
	 * Retrieve all of a repo's github commands.
	 */
	public List<GithubCommand> getCommands(RepoId repoId) {
		return databaseStorage.acquireReadTransaction(db -> {
			return db.dsl()
				.selectFrom(GITHUB_COMMAND)
				.where(GITHUB_COMMAND.REPO_ID.eq(repoId.getIdAsString()))
				.stream()
				.map(record -> new GithubCommand(
					RepoId.fromString(record.getRepoId()),
					record.getPr(),
					BranchName.fromName(record.getTargetBranch()),
					record.getComment(),
					new CommitHash(record.getCommitHash()),
					GithubCommandState.fromTextualRepresentation(record.getState()),
					record.getTriesLeft()
				))
				.collect(toList());
		});
	}
}
