package de.aaaaaaah.velcom.backend.access.repoaccess.entities;

import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;

public class SearchBranchDescription {

	private final RepoId repoId;
	private final BranchName name;
	private final CommitHash commitHash;
	private final String commitSummary;
	private final boolean hasRun;

	public SearchBranchDescription(RepoId repoId, BranchName name, CommitHash commitHash,
		String commitSummary, boolean hasRun) {

		this.repoId = repoId;
		this.name = name;
		this.commitHash = commitHash;
		this.commitSummary = commitSummary;
		this.hasRun = hasRun;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public BranchName getName() {
		return name;
	}

	public CommitHash getCommitHash() {
		return commitHash;
	}

	public String getCommitSummary() {
		return commitSummary;
	}

	public boolean hasRun() {
		return hasRun;
	}

	@Override
	public String toString() {
		return "SearchBranchDescription{" +
			"repoId=" + repoId +
			", name=" + name +
			", commitHash=" + commitHash +
			", commitSummary='" + commitSummary + '\'' +
			", hasRun=" + hasRun +
			'}';
	}
}
