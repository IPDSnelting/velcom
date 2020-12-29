package de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities;

import javax.annotation.Nullable;

public class ShortRunDescription {

	private final RunId id;
	@Nullable
	private final String commitSummary;
	@Nullable
	private final String tarDescription;

	public ShortRunDescription(RunId id, @Nullable String commitSummary,
		@Nullable String tarDescription) {

		this.id = id;
		this.commitSummary = commitSummary;
		this.tarDescription = tarDescription;
	}

	public RunId getId() {
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
