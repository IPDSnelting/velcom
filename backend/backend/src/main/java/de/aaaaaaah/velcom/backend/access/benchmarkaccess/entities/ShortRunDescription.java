package de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities;

import java.util.Objects;
import javax.annotation.Nullable;

public class ShortRunDescription {

	private final RunId id;

	// TODO: 2020-12-30 Use Either
	@Nullable
	private final String commitHash;
	@Nullable
	private final String commitSummary;
	@Nullable
	private final String tarDescription;

	public ShortRunDescription(RunId id, @Nullable String commitHash, @Nullable String commitSummary,
		@Nullable String tarDescription) {

		this.id = id;
		// TODO: 2020-12-30 Use commit summary instead of entire message
		this.commitHash = commitHash;
		this.commitSummary = commitSummary;
		this.tarDescription = tarDescription;
	}

	public RunId getId() {
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ShortRunDescription that = (ShortRunDescription) o;
		return Objects.equals(id, that.id) && Objects
			.equals(commitSummary, that.commitSummary) && Objects
			.equals(tarDescription, that.tarDescription);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, commitSummary, tarDescription);
	}

	@Override
	public String toString() {
		return "ShortRunDescription{" +
			"id=" + id +
			", commitSummary='" + commitSummary + '\'' +
			", tarDescription='" + tarDescription + '\'' +
			'}';
	}
}
