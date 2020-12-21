package de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities;

import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Either;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
	private final Either<CommitSource, TarSource> source;
	private final Either<RunError, Collection<Measurement>> result;

	public Run(RunId id, String author, String runnerName, String runnerInfo, Instant startTime,
		Instant stopTime, Either<CommitSource, TarSource> source,
		Either<RunError, Collection<Measurement>> result) {

		this.id = Objects.requireNonNull(id);
		this.author = Objects.requireNonNull(author);
		this.runnerName = Objects.requireNonNull(runnerName);
		this.runnerInfo = Objects.requireNonNull(runnerInfo);
		this.startTime = Objects.requireNonNull(startTime);
		this.stopTime = Objects.requireNonNull(stopTime);
		this.source = Objects.requireNonNull(source);
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

	public Either<CommitSource, TarSource> getSource() {
		return source;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getStopTime() {
		return stopTime;
	}

	public Either<RunError, Collection<Measurement>> getResult() {
		return result;
	}

	public Optional<RepoId> getRepoId() {
		return getSource().consume(
			commitSource -> Optional.of(commitSource.getRepoId()),
			TarSource::getRepoId
		);
	}

	public Set<Dimension> getAllDimensionsUsed() {
		return result.getRight().stream()
			.flatMap(Collection::stream)
			.map(Measurement::getDimension)
			.collect(Collectors.toSet());
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
			", result=" + result +
			'}';
	}
}

