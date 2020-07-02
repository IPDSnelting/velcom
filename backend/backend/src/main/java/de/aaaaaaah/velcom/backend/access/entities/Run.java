package de.aaaaaaah.velcom.backend.access.entities;

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
	private final String author;
	private final String runnerName;
	private final String runnerInfo;
	private final Instant startTime;
	private final Instant stopTime;
	private final Optional<RepoSource> repoSource;
	private final Optional<String> errorMessage;
	private final Optional<Collection<Measurement>> measurements;

	public Run(RunId runId, String author, String runnerName, String runnerInfo, Instant startTime,
		Instant stopTime, @Nullable RepoSource repoSource, Collection<Measurement> measurements) {

		this(runId, author, runnerName, runnerInfo, startTime, stopTime, repoSource, null,
			measurements);
	}

	public Run(RunId runId, String author, String runnerName, String runnerInfo, Instant startTime,
		Instant stopTime, Collection<Measurement> measurements) {

		this(runId, author, runnerName, runnerInfo, startTime, stopTime, null, null, measurements);
	}

	public Run(RunId runId, String author, String runnerName, String runnerInfo, Instant startTime,
		Instant stopTime, @Nullable  RepoSource repoSource, String errorMessage) {

		this(runId, author, runnerName, runnerInfo, startTime, stopTime, repoSource, errorMessage,
			null);
	}

	public Run(RunId runId, String author, String runnerName, String runnerInfo, Instant startTime,
		Instant stopTime, String errorMessage) {

		this(runId, author, runnerName, runnerInfo, startTime, stopTime, null, errorMessage, null);
	}

	private Run(RunId id, String author, String runnerName, String runnerInfo,
		Instant startTime, Instant stopTime,
		@Nullable RepoSource repoSource, @Nullable String errorMessage,
		@Nullable Collection<Measurement> measurements) {
		this.id = Objects.requireNonNull(id);
		this.author = Objects.requireNonNull(author);
		this.runnerName = Objects.requireNonNull(runnerName);
		this.runnerInfo = Objects.requireNonNull(runnerInfo);
		this.startTime = Objects.requireNonNull(startTime);
		this.stopTime = Objects.requireNonNull(stopTime);

		this.repoSource = Optional.ofNullable(repoSource);
		this.errorMessage = Optional.ofNullable(errorMessage);
		this.measurements = Optional.ofNullable(measurements);
	}

	public RunId getId() {
		return id;
	}

	public String getAuthor() {
		return author;
	}

	public String getRunnerName() {
		return runnerName;
	}

	public String getRunnerInfo() {
		return runnerInfo;
	}

	public Optional<RepoSource> getRepoSource() {
		return repoSource;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getStopTime() {
		return stopTime;
	}

	public Optional<String> getErrorMessage() {
		return errorMessage;
	}

	public Optional<Collection<Measurement>> getMeasurements() {
		return measurements;
	}

	@Override
	public String toString() {
		return "Run{" +
			"id=" + id +
			", author='" + author + '\'' +
			", runnerName='" + runnerName + '\'' +
			", runnerInfo='" + runnerInfo + '\'' +
			", startTime=" + startTime +
			", stopTime=" + stopTime +
			", repoSource=" + repoSource +
			", errorMessage=" + errorMessage +
			", measurements=" + measurements +
			'}';
	}

}

