package de.aaaaaaah.velcom.backend.data.linearlog;

import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Branch;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.exceptions.CommitLogException;
import de.aaaaaaah.velcom.shared.util.Pair;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This is a linear log that is based on the log at {@link CommitReadAccess#getCommitLog(RepoId,
 * Collection)}, which is based on jgit's git log command.
 */
public class CommitAccessBasedLinearLog implements LinearLog {

	private final CommitReadAccess commitAccess;
	private final RepoReadAccess repoAccess;

	public CommitAccessBasedLinearLog(CommitReadAccess commitAccess, RepoReadAccess repoAccess) {
		this.commitAccess = commitAccess;
		this.repoAccess = repoAccess;
	}

	@Override
	public Stream<Commit> walk(RepoId repoId, Collection<BranchName> branches)
		throws LinearLogException {

		try {
			return commitAccess.getCommitLog(repoId, branches);
		} catch (CommitLogException e) {
			throw new LinearLogException(e);
		}
	}

	@Override
	public Optional<Commit> getPreviousCommit(Commit commit) {
		RepoId repoId = commit.getRepoId();
		Collection<Branch> trackedBranches = repoAccess.getTrackedBranches(repoId);

		try (Stream<Commit> walk = walkBranches(repoId, trackedBranches)) {
			Iterator<Commit> remainingCommits = walk
				.dropWhile(c -> !commit.equals(c))
				.skip(1)
				.iterator();

			return remainingCommits.hasNext()
				? Optional.of(remainingCommits.next())
				: Optional.empty();
		} catch (LinearLogException e) {
			return Optional.empty();
		}
	}

	@Override
	public Pair<Optional<Commit>, Optional<Commit>> getPrevNextCommits(Commit commit) {
		RepoId repoId = commit.getRepoId();
		Collection<Branch> trackedBranches = repoAccess.getTrackedBranches(repoId);

		try (Stream<Commit> walk = walkBranches(repoId, trackedBranches)) {
			Iterator<Commit> iterator = walk.iterator();

			boolean foundCommit = false;
			Commit nextCommit = null;
			while (iterator.hasNext()) {
				Commit currentCommit = iterator.next();
				if (currentCommit.getHash().equals(commit.getHash())) {
					foundCommit = true;
					break;
				} else {
					nextCommit = currentCommit;
				}
			}

			if (!foundCommit) {
				return new Pair<>(Optional.empty(), Optional.empty());
			}

			Commit prevCommit = null;
			if (iterator.hasNext()) {
				prevCommit = iterator.next();
			}

			return new Pair<>(Optional.ofNullable(prevCommit), Optional.ofNullable(nextCommit));
		} catch (LinearLogException e) {
			return new Pair<>(Optional.empty(), Optional.empty());
		}

	}

	// TODO maybe optimize getPreviousCommit using jgit in CommitAccess?

}
