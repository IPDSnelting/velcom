package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Objects;
import java.util.UUID;

public class RepoId {

	private final UUID id;

	public RepoId(UUID id) {
		this.id = id;
	}

	public RepoId() {
		this(UUID.randomUUID());
	}

	public String getDirectoryName() {
		return getId().toString();
	}

	public UUID getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RepoId repoId = (RepoId) o;
		return Objects.equals(id, repoId.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "RepoId{" +
			"id=" + id +
			'}';
	}
}
