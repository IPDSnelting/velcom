package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Optional;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A basic builder that allows the creation of new runs.
 */
public class RunBuilder {

	/**
	 * Creates a new run builder for successful runs.
	 *
	 * @param task the task that prompted the run
	 * @param runnerName the name of the runner that performed the run
	 * @param runnerInfo some additional info about that runner
	 * @param startTime the point in time when the runner started the benchmark script
	 * @param stopTime the point in time when the benchmark script exited
	 * @return a new builder instance
	 */
	public static RunBuilder successful(Task task, String runnerName, String runnerInfo,
		Instant startTime, Instant stopTime) {
		return new RunBuilder(
			new RunId(task.getId().getId()),
			task.getAuthor(),
			runnerName,
			runnerInfo,
			startTime,
			stopTime,
			task.getSource().getLeft().orElse(null),
			null
		);
	}

	/**
	 * Creates a new run builder for failed runs.
	 *
	 * @param task the task that prompted the run
	 * @param runnerName the name of the runner that performed the run
	 * @param runnerInfo some additional info about that runner
	 * @param startTime the start time
	 * @param stopTime the stop time
	 * @param errorMessage the error message
	 * @param errorType what kind of error occured
	 * @return a new builder instance
	 */
	public static RunBuilder failed(Task task, String runnerName, String runnerInfo,
		Instant startTime, Instant stopTime, String errorMessage, ErrorType errorType) {
		return new RunBuilder(
			new RunId(task.getId().getId()),
			task.getAuthor(),
			runnerName,
			runnerInfo,
			startTime,
			stopTime,
			task.getSource().getLeft().orElse(null),
			new RunError(errorMessage, errorType)
		);
	}

	private final RunId runId;
	private final String author;
	private final String runnerName;
	private final String runnerInfo;
	private final Instant startTime;
	private final Instant stopTime;
	private final Optional<RepoSource> repoSource;
	private final List<Measurement> measurementList;
	private final Optional<RunError> error;

	private RunBuilder(RunId runId, String author, String runnerName, String runnerInfo,
		Instant startTime, Instant stopTime,
		@Nullable RepoSource repoSource, @Nullable RunError error) {

		this.runId = runId;
		this.author = Objects.requireNonNull(author);
		this.runnerName = Objects.requireNonNull(runnerName);
		this.runnerInfo = Objects.requireNonNull(runnerInfo);
		this.startTime = Objects.requireNonNull(startTime);
		this.stopTime = Objects.requireNonNull(stopTime);
		this.repoSource = Optional.ofNullable(repoSource);
		this.error = Optional.ofNullable(error);
		this.measurementList = error != null ? Collections.emptyList() : new ArrayList<>();
	}

	/**
	 * Adds a new successful measurement to the run.
	 *
	 * @param name the name of the measurement
	 * @param interpretation the interpretation
	 * @param unit the unit
	 * @param values the values
	 */
	public void addSuccessfulMeasurement(MeasurementName name, Interpretation interpretation,
		Unit unit, List<Double> values) {

		MeasurementValues measurementValues = new MeasurementValues(values, unit, interpretation);
		Measurement measurement = new Measurement(this.runId, name, measurementValues);
		this.measurementList.add(measurement);
	}

	/**
	 * Adds a new failed measurement to the run.
	 *
	 * @param name the name of the measurement
	 * @param errorMessage the error message
	 */
	public void addFailedMeasurement(MeasurementName name, String errorMessage) {

		MeasurementError measurementError = new MeasurementError(errorMessage);
		Measurement measurement = new Measurement(this.runId, name, measurementError);
		this.measurementList.add(measurement);
	}

	/**
	 * Builds the specified run instance.
	 *
	 * @return the created run instance
	 * @throws IllegalStateException if this run cannot be built yet.
	 */
	public Run build() throws IllegalStateException {
		if (this.error.isPresent()) {
			return new Run(
				runId,
				author,
				runnerName,
				runnerInfo,
				startTime,
				stopTime,
				repoSource.orElse(null),
				this.error.get()
			);
		} else {
			if (this.measurementList.isEmpty()) {
				throw new IllegalStateException("measurement list is empty for: " + this);
			}

			return new Run(
				runId,
				author,
				runnerName,
				runnerInfo,
				startTime,
				stopTime,
				repoSource.orElse(null),
				measurementList
			);
		}
	}

	@Override
	public String toString() {
		return "RunBuilder{" +
			"runId=" + runId +
			", author='" + author + '\'' +
			", runnerName='" + runnerName + '\'' +
			", runnerInfo='" + runnerInfo + '\'' +
			", startTime=" + startTime +
			", stopTime=" + stopTime +
			", repoSource=" + repoSource +
			", measurementList=" + measurementList +
			", error=" + error +
			'}';
	}

}
