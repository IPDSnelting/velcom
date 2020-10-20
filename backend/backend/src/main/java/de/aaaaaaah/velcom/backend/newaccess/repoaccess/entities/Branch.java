package de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities;

import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import java.util.Objects;

public class Branch {

	private final RepoId repoId;
	private final BranchName name;
	private final CommitHash latestCommitHash;
	private final boolean tracked;

	public Branch(RepoId repoId, BranchName name, CommitHash latestCommitHash, boolean tracked) {
		this.repoId = repoId;
		this.name = name;
		this.latestCommitHash = latestCommitHash;
		this.tracked = tracked;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public BranchName getName() {
		return name;
	}

	public CommitHash getLatestCommitHash() {
		return latestCommitHash;
	}

	public boolean isTracked() {
		return tracked;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Branch branch = (Branch) o;
		return Objects.equals(repoId, branch.repoId) &&
			Objects.equals(name, branch.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(repoId, name);
	}

	@Override
	public String toString() {
		return "Branch{" +
			"repoId=" + repoId +
			", branchName=" + name +
			", latestCommitHash=" + latestCommitHash +
			", tracked=" + tracked +
			'}';
	}
}
