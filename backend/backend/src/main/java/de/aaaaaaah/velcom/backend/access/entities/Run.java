package de.aaaaaaah.velcom.backend.access.entities;

import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.entities.sources.TarSource;
import de.aaaaaaah.velcom.shared.util.Either;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

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

	public Run(RunId runId, String author, String runnerName, String runnerInfo, Instant startTime,
		Instant stopTime, Either<CommitSource, TarSource> source,
		Collection<Measurement> measurements) {

		this(runId, author, runnerName, runnerInfo, startTime, stopTime, source, null, measurements);
	}

	public Run(RunId runId, String author, String runnerName, String runnerInfo, Instant startTime,
		Instant stopTime, Either<CommitSource, TarSource> source, RunError error) {

		this(runId, author, runnerName, runnerInfo, startTime, stopTime, source, error, null);
	}

	private Run(RunId id, String author, String runnerName, String runnerInfo, Instant startTime,
		Instant stopTime, Either<CommitSource, TarSource> source, @Nullable RunError error,
		@Nullable Collection<Measurement> measurements) {

		this.id = Objects.requireNonNull(id);
		this.author = Objects.requireNonNull(author);
		this.runnerName = Objects.requireNonNull(runnerName);
		this.runnerInfo = Objects.requireNonNull(runnerInfo);
		this.startTime = Objects.requireNonNull(startTime);
		this.stopTime = Objects.requireNonNull(stopTime);
		this.source = Objects.requireNonNull(source);

		if (error != null && measurements != null) {
			throw new IllegalArgumentException(
				"either error or measurement must be present, but not both at the same time!"
			);
		} else if (error != null) {
			this.result = Either.ofLeft(error);
		} else if (measurements != null) {
			this.result = Either.ofRight(measurements);
		} else {
			throw new IllegalArgumentException("both error and measurement are null");
		}
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
		if (getSource().isLeft()) {
			return Optional.of(getSource().getLeft().get().getRepoId());
		} else {
			return getSource().getRight().get().getRepoId();
		}
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
			", repoSource=" + source +
			", result=" + result +
			'}';
	}

}

