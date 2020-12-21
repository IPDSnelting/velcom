package de.aaaaaaah.velcom.backend.access.repoaccess.entities;

import java.util.Objects;
import java.util.UUID;

/**
 * The information velcom knows about a repository it tracks.
 */
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

	public UUID getIdAsUuid() {
		return id.getId();
	}

	public String getIdAsString() {
		return id.getIdAsString();
	}

	public String getName() {
		return name;
	}

	public RemoteUrl getRemoteUrl() {
		return remoteUrl;
	}

	public String getRemoteUrlAsString() {
		return remoteUrl.getUrl();
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
