package de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities;

import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.shared.util.Either;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public class Run {

	private final RunId id;
	private final String author;
	private final String runnerName;
	private final String runnerInfo;
	private final Instant startTime;
	private final Instant stopTime;
	private final Either<CommitSource, TarSource> source;
	@Nullable
	private final RunError error;

	public Run(RunId id, String author, String runnerName, String runnerInfo, Instant startTime,
		Instant stopTime, Either<CommitSource, TarSource> source, @Nullable RunError error) {

		this.id = id;
		this.author = author;
		this.runnerName = runnerName;
		this.runnerInfo = runnerInfo;
		this.startTime = startTime;
		this.stopTime = stopTime;
		this.source = source;
		this.error = error;
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

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getStopTime() {
		return stopTime;
	}

	public Either<CommitSource, TarSource> getSource() {
		return source;
	}

	public Optional<RunError> getError() {
		return Optional.ofNullable(error);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Run run = (Run) o;
		return Objects.equals(id, run.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
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
			", source=" + source +
			", error=" + error +
			'}';
	}
}
