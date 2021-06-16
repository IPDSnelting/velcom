package de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities;

import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.shared.util.Pair;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public class ShortRunDescription {

	private final RunId id;

	// Not using an Either since this data type's abstraction level is pretty low and in the places it
	// is used, we don't really need the guarantees that would give us.
	@Nullable
	private final String commitHash;
	@Nullable
	private final String commitSummary;
	@Nullable
	private final String tarDescription;

	public ShortRunDescription(RunId id, @Nullable String commitHash, @Nullable String commitMessage,
		@Nullable String tarDescription) {

		boolean commitHashNull = commitHash == null;
		boolean commitMessageNull = commitMessage == null;
		boolean tarDescriptionNull = tarDescription == null;
		if (commitHashNull != commitMessageNull) {
			throw new IllegalArgumentException(
				"commitHash and commitMessage must either both be null or non-null");
		}
		if (commitHashNull == tarDescriptionNull) {
			throw new IllegalArgumentException(
				"exactly one of commitHash and tarDescription must be null");
		}

		this.id = id;
		this.commitHash = commitHash;
		this.commitSummary = Optional.ofNullable(commitMessage)
			.map(Commit::splitMessageIntoSections)
			.map(Pair::getFirst)
			.orElse(null);
		this.tarDescription = tarDescription;
	}

	public RunId getId() {
		return id;
	}

	public Optional<String> getCommitHash() {
		return Optional.ofNullable(commitHash);
	}

	public Optional<String> getCommitSummary() {
		return Optional.ofNullable(commitSummary);
	}

	public Optional<String> getTarDescription() {
		return Optional.ofNullable(tarDescription);
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
