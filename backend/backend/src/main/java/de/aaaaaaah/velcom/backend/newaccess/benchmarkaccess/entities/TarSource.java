package de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities;

import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public class TarSource {

	private final String description;
	@Nullable
	private final RepoId repoId;

	public TarSource(String description) {
		this(description, null);
	}

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
	public String toString() {
		return "TarSource{" +
			"description='" + description + '\'' +
			", repoId=" + repoId +
			'}';
	}

}
