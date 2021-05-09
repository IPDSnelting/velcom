package de.aaaaaaah.velcom.backend.listener.github;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;

public class FinishedGithubCommand {

	private final long pr;
	private final CommitHash commitHash;
	private final RunId runId;

	public FinishedGithubCommand(long pr, CommitHash commitHash, RunId runId) {
		this.pr = pr;
		this.commitHash = commitHash;
		this.runId = runId;
	}

	public long getPr() {
		return pr;
	}

	public CommitHash getCommitHash() {
		return commitHash;
	}

	public RunId getRunId() {
		return runId;
	}
}
