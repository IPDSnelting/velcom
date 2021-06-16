package de.aaaaaaah.velcom.backend.access.repoaccess.entities;

import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import java.util.Objects;

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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SearchBranchDescription that = (SearchBranchDescription) o;
		return hasRun == that.hasRun && Objects.equals(repoId, that.repoId) && Objects
			.equals(name, that.name) && Objects.equals(commitHash, that.commitHash)
			&& Objects.equals(commitSummary, that.commitSummary);
	}

	@Override
	public int hashCode() {
		return Objects.hash(repoId, name, commitHash, commitSummary, hasRun);
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
