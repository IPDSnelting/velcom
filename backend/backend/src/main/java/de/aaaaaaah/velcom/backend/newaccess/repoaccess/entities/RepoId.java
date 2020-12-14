package de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities;

import java.util.Objects;
import java.util.UUID;

/**
 * A unique identifier for a repo.
 */
public class RepoId implements Comparable<RepoId> {

	private final UUID id;

	public RepoId(UUID id) {
		this.id = id;
	}

	/**
	 * Create a new, random {@link RepoId}.
	 */
	public RepoId() {
		this(UUID.randomUUID());
	}

	/**
	 * Create a new {@link RepoId} from a UUID string.
	 *
	 * @param string the UUID as string
	 * @return the repo id
	 */
	public static RepoId fromString(String string) {
		return new RepoId(UUID.fromString(string));
	}

	public String getDirectoryName() {
		return getIdAsString();
	}

	public UUID getId() {
		return id;
	}

	public String getIdAsString() {
		return id.toString();
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

	@Override
	public int compareTo(RepoId other) {
		if (other == null) {
			return 1;
		}
		return this.getId().toString().compareTo(other.getId().toString());
	}

}
