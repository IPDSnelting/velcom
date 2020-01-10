package de.aaaaaaah.velcom.backend.data.linearlog;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccessException;
import de.aaaaaaah.velcom.backend.access.repo.BranchName;
import de.aaaaaaah.velcom.backend.access.repo.Repo;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * This is a linear log that is based on the log at {@link CommitAccess#getCommitLog(Repo,
 * Collection)}, which is based on jgit's git log command.
 */
public class CommitAccessBasedLinearLog implements LinearLog {

	private final CommitAccess commitAccess;

	public CommitAccessBasedLinearLog(CommitAccess commitAccess) {
		this.commitAccess = commitAccess;
	}

	@Override
	public Stream<Commit> walk(Repo repo, Collection<BranchName> branches)
		throws LinearLogException {

		try {
			return commitAccess.getCommitLog(repo, branches);
		} catch (CommitAccessException e) {
			throw new LinearLogException(e);
		}
	}

	// TODO maybe optimize getPreviousCommit using jgit in CommitAccess?
}
