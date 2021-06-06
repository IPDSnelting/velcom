package de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities;

import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import java.time.Instant;
import java.util.Optional;
import javax.annotation.Nullable;

public class SearchRunDescription {

	private final RunId id;
	@Nullable
	private final RepoId repoId;
	@Nullable
	private final String commitSummary;
	@Nullable
	private final String tarDescription;
	private final Instant startTime;
	private final Instant stopTime;

	public SearchRunDescription(RunId id, @Nullable RepoId repoId, @Nullable String commitSummary,
		@Nullable String tarDescription, Instant startTime, Instant stopTime) {

		if (commitSummary != null && repoId == null) {
			throw new IllegalArgumentException("repoId must not be null if commitSummary exists");
		}
		if ((commitSummary == null) == (tarDescription == null)) {
			throw new IllegalArgumentException(
				"exactly one of commitSummary and tarDescription must be null");
		}

		this.id = id;
		this.repoId = repoId;
		this.commitSummary = commitSummary;
		this.tarDescription = tarDescription;
		this.startTime = startTime;
		this.stopTime = stopTime;
	}

	public RunId getId() {
		return id;
	}

	public Optional<RepoId> getRepoId() {
		return Optional.ofNullable(repoId);
	}

	public Optional<String> getCommitSummary() {
		return Optional.ofNullable(commitSummary);
	}

	public Optional<String> getTarDescription() {
		return Optional.ofNullable(tarDescription);
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getStopTime() {
		return stopTime;
	}

	@Override
	public String toString() {
		return "SearchRunDescription{" +
			"id=" + id +
			", repoId=" + repoId +
			", commitSummary='" + commitSummary + '\'' +
			", tarDescription='" + tarDescription + '\'' +
			", startTime=" + startTime +
			", stopTime=" + stopTime +
			'}';
	}
}
