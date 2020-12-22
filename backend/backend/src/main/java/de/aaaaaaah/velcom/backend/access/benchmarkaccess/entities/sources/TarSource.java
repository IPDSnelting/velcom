package de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources;

import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A source describing that the task originated from a tar. The task may be attached to a repo.
 */
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
	public String toString() {
		return "TarSource{" +
			"description='" + description + '\'' +
			", repoId=" + repoId +
			'}';
	}

}
