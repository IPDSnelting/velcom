package de.aaaaaaah.designproto.backend.access.commit;

import java.util.Objects;

/**
 * The hash of a commit. Used (usually in conjunction with a {@link de.aaaaaaah.designproto.backend.access.repo.RepoId})
 * for identifying commits.
 */
public class CommitHash {

	private final String hash;

	public CommitHash(String hash) {
		this.hash = hash;
	}

	public String getHash() {
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
		CommitHash that = (CommitHash) o;
		return Objects.equals(hash, that.hash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(hash);
	}

	@Override
	public String toString() {
		return "CommitHash{" +
			"hash='" + hash + '\'' +
			'}';
	}
}
