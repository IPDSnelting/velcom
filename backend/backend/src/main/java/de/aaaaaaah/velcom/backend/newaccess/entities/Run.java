package de.aaaaaaah.velcom.backend.newaccess.entities;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * A run is a single execution of the benchmark script.
 *
 * <p> It can either be successful, in which case it may contain multiple successful and failed
 * {@link Measurement}s, or it can be failed, in which case it only contains an error message and no
 * {@link Measurement}s. Specifically, a successful {@link Run} can still contain failed {@link
 * Measurement}s.
 */
public class Run {

	private final RunId id;
	private final RepoId repoId;
	private final CommitHash commitHash;
	private final Instant startTime;
	private final Instant stopTime;

	@Nullable
	private final String errorMessage;
	@Nullable
	private final Collection<Measurement> measurements;

	public Run(RunId id, RepoId repoId, CommitHash commitHash, Instant startTime, Instant stopTime,
	           Collection<Measurement> measurements) {
		this(id, repoId, commitHash, startTime, stopTime, null, measurements);
	}

	public Run(RunId id, RepoId repoId, CommitHash commitHash, Instant startTime, Instant stopTime,
	           String errorMessage) {
		this(id, repoId, commitHash, startTime, stopTime, errorMessage, null);
	}

	private Run(RunId id, RepoId repoId, CommitHash commitHash, Instant startTime, Instant stopTime,
	            @Nullable String errorMessage, @Nullable Collection<Measurement> measurements) {

		this.id = Objects.requireNonNull(id);
		this.repoId = Objects.requireNonNull(repoId);
		this.commitHash = Objects.requireNonNull(commitHash);
		this.startTime = Objects.requireNonNull(startTime);
		this.stopTime = Objects.requireNonNull(stopTime);
		this.errorMessage = errorMessage;
		this.measurements = measurements;
	}

	public RunId getId() {
		return id;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public CommitHash getCommitHash() {
		return commitHash;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getStopTime() {
		return stopTime;
	}

	@Nullable
	public Optional<String> getErrorMessage() {
		return Optional.ofNullable(errorMessage);
	}

	@Nullable
	public Optional<Collection<Measurement>> getMeasurements() {
		return Optional.ofNullable(measurements);
	}

	@Override
	public String toString() {
		return "Run{" +
			"id=" + id +
			", repoId=" + repoId +
			", commitHash=" + commitHash +
			", startTime=" + startTime +
			", stopTime=" + stopTime +
			", errorMessage='" + errorMessage + '\'' +
			", measurements=" + measurements +
			'}';
	}

}

