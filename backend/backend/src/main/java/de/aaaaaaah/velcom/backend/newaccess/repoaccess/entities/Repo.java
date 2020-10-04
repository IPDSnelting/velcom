package de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities;

import de.aaaaaaah.velcom.backend.access.entities.RemoteUrl;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
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
			"id=" + id +
			", name='" + name + '\'' +
			", remoteUrl=" + remoteUrl +
			'}';
	}
}
