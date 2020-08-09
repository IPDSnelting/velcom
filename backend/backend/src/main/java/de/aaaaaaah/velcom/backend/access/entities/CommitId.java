package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Objects;

/**
 * A unique identifier for a single commit in a certain repository.
 */
public class CommitId {

	private final RepoId repoId;
	private final CommitHash hash;

	public CommitId(RepoId repoId, CommitHash hash) {
		this.repoId = Objects.requireNonNull(repoId);
		this.hash = Objects.requireNonNull(hash);
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public CommitHash getHash() {
		return hash;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		CommitId commitId = (CommitId) o;
		return repoId.equals(commitId.repoId) &&
			hash.equals(commitId.hash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(repoId, hash);
	}

	@Override
	public String toString() {
		return "CommitId{" +
			"repoId=" + repoId +
			", hash=" + hash +
			'}';
	}

}
