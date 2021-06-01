package de.aaaaaaah.velcom.backend.listener.github;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;

public class FinishedGithubCommand {

	private final long pr;
	private final BranchName targetBranch;
	private final CommitHash commitHash;
	private final RunId runId;

	public FinishedGithubCommand(long pr, BranchName targetBranch, CommitHash commitHash,
		RunId runId) {

		this.pr = pr;
		this.targetBranch = targetBranch;
		this.commitHash = commitHash;
		this.runId = runId;
	}

	public long getPr() {
		return pr;
	}

	public BranchName getTargetBranch() {
		return targetBranch;
	}

	public CommitHash getCommitHash() {
		return commitHash;
	}

	public RunId getRunId() {
		return runId;
	}
}
