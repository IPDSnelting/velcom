package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Objects;

/**
 * A source describing that the task originated from a commit in a repository.
 */
public class RepoSource {

	private final RepoId repoId;
	private final CommitHash hash;

	public RepoSource(RepoId repoId, CommitHash hash) {
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
		RepoSource source = (RepoSource) o;
		return repoId.equals(source.repoId) &&
			hash.equals(source.hash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(repoId, hash);
	}

	@Override
	public String toString() {
		return "RepoSource{" +
			"repoId=" + repoId +
			", hash=" + hash +
			'}';
	}

}
