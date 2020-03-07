package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Objects;

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
