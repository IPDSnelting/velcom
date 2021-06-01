package de.aaaaaaah.velcom.backend.listener.github;

import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import java.util.Objects;

public class GithubCommand {

	private static final int TRIES = 5;

	private final RepoId repoId;
	private final long pr;
	private final BranchName targetBranch;
	private final long comment;
	private final CommitHash commitHash;
	private final GithubCommandState state;
	private final int triesLeft;

	public GithubCommand(RepoId repoId, long pr, BranchName targetBranch, long comment,
		CommitHash commitHash, GithubCommandState state, int triesLeft) {

		this.repoId = repoId;
		this.pr = pr;
		this.targetBranch = targetBranch;
		this.comment = comment;
		this.commitHash = commitHash;
		this.state = state;
		this.triesLeft = triesLeft;
	}

	public GithubCommand(RepoId repoId, long pr, BranchName targetBranch, long comment,
		CommitHash commitHash) {

		this(repoId, pr, targetBranch, comment, commitHash, GithubCommandState.NEW, TRIES);
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public long getPr() {
		return pr;
	}

	public BranchName getTargetBranch() {
		return targetBranch;
	}

	public long getComment() {
		return comment;
	}

	public CommitHash getCommitHash() {
		return commitHash;
	}

	public GithubCommandState getState() {
		return state;
	}

	public int getTriesLeft() {
		return triesLeft;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		GithubCommand that = (GithubCommand) o;
		return pr == that.pr && comment == that.comment && triesLeft == that.triesLeft
			&& Objects.equals(repoId, that.repoId) && Objects
			.equals(commitHash, that.commitHash) && state == that.state;
	}

	@Override
	public int hashCode() {
		return Objects.hash(repoId, pr, comment, commitHash, state, triesLeft);
	}

	@Override
	public String toString() {
		return "GithubCommand{" +
			"repoId=" + repoId +
			", pr=" + pr +
			", comment=" + comment +
			", commitHash=" + commitHash +
			", state=" + state +
			", triesLeft=" + triesLeft +
			'}';
	}
}
