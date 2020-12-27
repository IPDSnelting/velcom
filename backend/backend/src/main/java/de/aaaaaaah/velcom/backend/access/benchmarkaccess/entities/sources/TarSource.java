package de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources;

import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public class TarSource {

	private final String description;
	@Nullable
	private final RepoId repoId;

	public TarSource(String description, @Nullable RepoId repoId) {
		this.description = Objects.requireNonNull(description);
		this.repoId = repoId;
	}

	public String getDescription() {
		return description;
	}

	public Optional<RepoId> getRepoId() {
		return Optional.ofNullable(repoId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TarSource tarSource = (TarSource) o;
		return Objects.equals(description, tarSource.description) && Objects
			.equals(repoId, tarSource.repoId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(description, repoId);
	}

	@Override
	public String toString() {
		return "TarSource{" +
			"description='" + description + '\'' +
			", repoId=" + repoId +
			'}';
	}

}
