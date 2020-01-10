package de.aaaaaaah.velcom.backend.access.commit;

import de.aaaaaaah.velcom.backend.access.AccessLayer;
import de.aaaaaaah.velcom.backend.access.repo.Branch;
import de.aaaaaaah.velcom.backend.access.repo.BranchName;
import de.aaaaaaah.velcom.backend.access.repo.Repo;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;
import de.aaaaaaah.velcom.backend.storage.repo.exception.RepositoryAcquisitionException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * This class abstracts away access to commits and the commit history of repositories.
 */
public class CommitAccess {

	private final AccessLayer accessLayer;
	private final DatabaseStorage databaseStorage;
	private final RepoStorage repoStorage;

	/**
	 * This constructor also registers the {@link CommitAccess} in the accessLayer.
	 *
	 * @param accessLayer the {@link AccessLayer} to register with
	 * @param databaseStorage a database storage
	 * @param repoStorage a repo storage
	 */
	public CommitAccess(AccessLayer accessLayer, DatabaseStorage databaseStorage,
		RepoStorage repoStorage) {

		this.accessLayer = accessLayer;
		this.databaseStorage = databaseStorage;
		this.repoStorage = repoStorage;

		accessLayer.registerCommitAccess(this);
	}

	// Commit operations

	public Commit getCommit(RepoId repoId, CommitHash commitHash) {
		// TODO implement
		return null;
	}

	public Collection<Commit> getCommits(RepoId repoId, Collection<CommitHash> commitHashes) {
		// TODO implement
		return null;
	}

	// Mutable properties

	/**
	 * See {@link Commit#isKnown()}. To make an unknown commit known, use {@link
	 * #setBenchmarkStatus(RepoId, CommitHash, BenchmarkStatus)} to set its benchmark status. To
	 * make a known commit unknown, use {@link #makeUnknown(RepoId, CommitHash)}.
	 *
	 * @param repoId the repo the commit is in
	 * @param hash the commit's hash
	 * @return whether the commit is known. Defaults to false if the repo id is invalid
	 */
	public boolean isKnown(RepoId repoId, CommitHash hash) {
		// TODO implement
		return false;
	}

	/**
	 * Remove a commit from the list of known commits. This also deletes associated information,
	 * such as the commit's benchmark status.
	 *
	 * @param repoId the repo the commit is in
	 * @param hash the commit's hash
	 */
	public void makeUnknown(RepoId repoId, CommitHash hash) {
		// TODO implement
	}

	/**
	 * Checks whether or not any of specified repository's commits are known at this point.
	 *
	 * @param repoId the id of the repository
	 * @return true if at least one commit of the repository is known
	 */
	public boolean hasKnownCommits(RepoId repoId) {
		// TODO implement
		return false;
	}

	public BenchmarkStatus getBenchmarkStatus(RepoId repoId, CommitHash commitHash) {
		return null; // TODO implement
	}

	public Task setBenchmarkStatus(RepoId repoId, CommitHash commitHash,
		BenchmarkStatus benchmarkStatus) {

		// TODO implement
		return null;
	}

	public Collection<Task> getAllTasksOfStatus(RepoId repoId, BenchmarkStatus status) {
		// TODO implement
		return null;
	}

	// Advanced operations

	public Collection<Commit> getAllCommitsRequiringBenchmark() {
		// TODO implement
		return null;
	}

	private Commit commitFromRevCommit(RepoId repoId, RevCommit revCommit) {
		CommitHash ownHash = new CommitHash(revCommit.getId().toString());
		List<CommitHash> parentHashes = List.of(revCommit.getParents()).stream()
			.map(RevCommit::getId)
			.map(AnyObjectId::toString)
			.map(CommitHash::new)
			.collect(Collectors.toUnmodifiableList());

		PersonIdent author = revCommit.getAuthorIdent();
		PersonIdent committer = revCommit.getCommitterIdent();

		return new Commit(
			accessLayer.getCommitAccess(),
			accessLayer.getRepoAccess(),
			repoId,
			ownHash,
			parentHashes,
			author.toExternalString(),
			author.getWhen().toInstant(),
			committer.toExternalString(),
			committer.getWhen().toInstant(),
			revCommit.getFullMessage()
		);
	}

	// TODO find out more about jgit's commit order
	// TODO What about the RepoStorage lock? Is the CommitAccessException enough?
	// TODO Make CommitAccessException unckecked?

	public CommitWalk getCommitWalk(Branch branch) {
		return null;
	}

	public CommitWalk getCommitWalk(Repo repo, Commit startCommit) {
		return null;
	}

	/**
	 * Returns all commits in the specified branches in the order that jgit puts them in. The stream
	 * must be closed manually once it is no longer required.
	 *
	 * @param repo the repo to take the commits from
	 * @param branches the branches to take the commits from
	 * @return the commits
	 * @throws NoSuchCommitException if anything goes wrong in the underlying jgit commit
	 * 	traversal
	 */
	public Stream<Commit> getCommitLog(Repo repo, Collection<BranchName> branches)
		throws CommitAccessException {

		// Step 1: Acquire repository
		Repository jgitRepo;

		try {
			String directoryName = repo.getId().getDirectoryName();
			jgitRepo = repoStorage.acquireRepository(directoryName);
		} catch (RepositoryAcquisitionException e) {
			throw new CommitAccessException(e);
		}

		try {
			// Step 2: Run log command
			LogCommand logCommand = Git.wrap(jgitRepo).log();

			for (BranchName branchName : branches) {

				ObjectId branchId = jgitRepo.resolve(branchName.getName());

				logCommand.add(branchId);
			}

			// Step 3: Prepare stream
			Spliterator<RevCommit> commitSpliterator = Spliterators.spliteratorUnknownSize(
				logCommand.call().iterator(), 0);

			return StreamSupport.stream(commitSpliterator, false)
				.map(revCommit -> commitFromRevCommit(repo.getId(), revCommit))
				.onClose(jgitRepo::close);
		} catch (IOException | GitAPIException e) {
			jgitRepo.close();
			throw new CommitAccessException(e);
		}
	}
}
