package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.ShortRunDescription;
import java.util.UUID;
import javax.annotation.Nullable;

public class JsonShortRunDescription {

	private final UUID id;
	@Nullable
	private final String commitSummary;
	@Nullable
	private final String tarDescription;

	public JsonShortRunDescription(UUID id, @Nullable String commitSummary,
		@Nullable String tarDescription) {

		this.id = id;
		this.commitSummary = commitSummary;
		this.tarDescription = tarDescription;
	}

	public static JsonShortRunDescription fromShortRunDescription(ShortRunDescription description) {
		return new JsonShortRunDescription(
			description.getId().getId(),
			description.getCommitSummary(),
			description.getTarDescription()
		);
	}

	public UUID getId() {
		return id;
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
