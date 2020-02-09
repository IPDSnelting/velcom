package de.aaaaaaah.velcom.backend.newaccess.entities;

public class Branch {

	private final RepoId repoId;
	private final BranchName name;

	public Branch(RepoId repoId, BranchName name) {
		this.repoId = repoId;
		this.name = name;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public BranchName getName() {
		return name;
	}

}
