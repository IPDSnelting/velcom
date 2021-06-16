package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.ShortRunDescription;
import java.util.UUID;
import javax.annotation.Nullable;

public class JsonShortRunDescription {

	private final UUID id;
	@Nullable
	private final String commitHash;
	@Nullable
	private final String commitSummary;
	@Nullable
	private final String tarDescription;

	public JsonShortRunDescription(UUID id, @Nullable String commitHash,
		@Nullable String commitSummary,
		@Nullable String tarDescription) {

		this.id = id;
		this.commitHash = commitHash;
		this.commitSummary = commitSummary;
		this.tarDescription = tarDescription;
	}

	public static JsonShortRunDescription fromShortRunDescription(ShortRunDescription description) {
		return new JsonShortRunDescription(
			description.getId().getId(),
			description.getCommitHash().orElse(null),
			description.getCommitSummary().orElse(null),
			description.getTarDescription().orElse(null)
		);
	}

	public UUID getId() {
		return id;
	}

	@Nullable
	public String getCommitHash() {
		return commitHash;
	}

	@Nullable
	public String getCommitSummary() {
		return commitSummary;
	}

	@Nullable
	public String getTarDescription() {
		return tarDescription;
	}
}
