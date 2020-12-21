package de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.builder;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Either;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

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

	public UUID getIdAsUuid() {
		return id.getId();
	}

	public String getIdAsString() {
		return id.getIdAsString();
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

	/**
	 * Convert this new run to an actual {@link Run}.
	 *
	 * @return the run
	 */
	public Run toRun() {
		Either<RunError, Collection<Measurement>> result = this.result.mapRight(
			newMeasurements -> newMeasurements.stream()
				.map(NewMeasurement::toMeasurement)
				.collect(toList())
		);

		return new Run(id, author, runnerName, runnerInfo, startTime, stopTime, source, result);
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
