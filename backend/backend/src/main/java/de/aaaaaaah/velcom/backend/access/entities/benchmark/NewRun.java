package de.aaaaaaah.velcom.backend.access.entities.benchmark;

import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunError;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.util.Either;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An entity used for adding new runs via the BenchmarkAccess.
 */
public class NewRun {

	private final RunId id;
	private final String author;
	private final String runnerName;
	private final String runnerInfo;
	private final Instant startTime;
	private final Instant stopTime;
	private final Either<CommitSource, TarSource> source;
	private final Either<RunError, Collection<NewMeasurement>> result;

	public NewRun(RunId id, String author, String runnerName, String runnerInfo, Instant startTime,
		Instant stopTime, Either<CommitSource, TarSource> source,
		RunError error) {

		this(id, author, runnerName, runnerInfo, startTime, stopTime, source, Either.ofLeft(error));
	}

	public NewRun(RunId id, String author, String runnerName, String runnerInfo, Instant startTime,
		Instant stopTime, Either<CommitSource, TarSource> source,
		Collection<NewMeasurement> measurements) {

		this(id, author, runnerName, runnerInfo, startTime, stopTime, source,
			Either.ofRight(measurements));
	}

	private NewRun(RunId id, String author, String runnerName, String runnerInfo, Instant startTime,
		Instant stopTime, Either<CommitSource, TarSource> source,
		Either<RunError, Collection<NewMeasurement>> result) {

		this.id = id;
		this.author = author;
		this.runnerName = runnerName;
		this.runnerInfo = runnerInfo;
		this.startTime = startTime;
		this.stopTime = stopTime;
		this.source = source;
		this.result = result;
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

	public Either<RunError, Collection<NewMeasurement>> getResult() {
		return result;
	}

	public Optional<RepoId> getRepoId() {
		if (getSource().isLeft()) {
			return Optional.of(getSource().getLeft().get().getRepoId());
		} else {
			return getSource().getRight().get().getRepoId();
		}
	}

	public Run toRun() {
		// TODO use fancy Either methods
		if (result.isLeft()) {
			RunError error = result.getLeft().get();
			return new Run(id, author, runnerName, runnerInfo, startTime, stopTime, source, error);
		} else {
			List<Measurement> measurements = result.getRight().get().stream()
				.map(NewMeasurement::toMeasurement)
				.collect(Collectors.toList());
			return new Run(id, author, runnerName, runnerInfo, startTime, stopTime, source, measurements);
		}
	}

	@Override
	public String toString() {
		return "NewRun{" +
			"id=" + id +
			", author='" + author + '\'' +
			", runnerName='" + runnerName + '\'' +
			", runnerInfo='" + runnerInfo + '\'' +
			", startTime=" + startTime +
			", stopTime=" + stopTime +
			", source=" + source +
			", result=" + result +
			'}';
	}
}
