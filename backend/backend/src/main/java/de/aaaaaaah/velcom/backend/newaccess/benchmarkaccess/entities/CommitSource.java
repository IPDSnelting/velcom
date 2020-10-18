package de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.Commit;
import java.util.Objects;

/**
 * A source describing that the task originated from a commit in a repository.
 */
public class CommitSource {

	private final RepoId repoId;
	private final CommitHash hash;

	public CommitSource(RepoId repoId, CommitHash hash) {
		this.repoId = Objects.requireNonNull(repoId);
		this.hash = Objects.requireNonNull(hash);
	}

	public static CommitSource fromCommit(Commit commit) {
		return new CommitSource(commit.getRepoId(), commit.getHash());
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
		CommitSource that = (CommitSource) o;
		return Objects.equals(repoId, that.repoId) &&
			Objects.equals(hash, that.hash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(repoId, hash);
	}

	@Override
	public String toString() {
		return "CommitSource{" +
			"repoId=" + repoId +
			", hash=" + hash +
			'}';
	}

}
