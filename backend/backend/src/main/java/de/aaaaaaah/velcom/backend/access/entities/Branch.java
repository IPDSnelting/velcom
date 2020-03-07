package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Objects;

/**
 * A branch that belongs to some repository.
 */
public class Branch {

	private final RepoId repoId;
	private final BranchName name;

	public Branch(RepoId repoId, BranchName name) {
		this.repoId = Objects.requireNonNull(repoId);
		this.name = Objects.requireNonNull(name);
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public BranchName getName() {
		return name;
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
		return repoId.equals(branch.repoId) &&
			name.equals(branch.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(repoId, name);
	}

	@Override
	public String toString() {
		return "Branch{" +
			"repoId=" + repoId +
			", name=" + name +
			'}';
	}

}
