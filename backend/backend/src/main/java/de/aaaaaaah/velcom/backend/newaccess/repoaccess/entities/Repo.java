package de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities;

import java.util.Objects;

public class Repo {

	private final RepoId id;
	private final String name;
	private final RemoteUrl remoteUrl;

	public Repo(RepoId id, String name, RemoteUrl remoteUrl) {
		this.id = id;
		this.name = name;
		this.remoteUrl = remoteUrl;
	}

	public RepoId getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	/**
	 * An alias for {@link #getId()} so initial diffs don't get too large...
	 *
	 * @return the repo's id
	 */
	// TODO: 04.10.20 Remove this function
	public RepoId getRepoId() {
		return id;
	}

	public RemoteUrl getRemoteUrl() {
		return remoteUrl;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Repo repo = (Repo) o;
		return Objects.equals(id, repo.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Repo{" +
			"id=" + id.getIdAsString() +
			", name='" + name + '\'' +
			", remoteUrl='" + remoteUrl.getUrl() + '\'' +
			'}';
	}
}
