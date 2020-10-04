package de.aaaaaaah.velcom.backend.access.entities;

import de.aaaaaaah.velcom.backend.access.entities.benchmark.NewMeasurement;
import de.aaaaaaah.velcom.backend.access.entities.benchmark.NewRun;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.shared.util.Either;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

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
			task.getSource(),
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
	 * @param runErrorType what kind of error occured
	 * @return a new builder instance
	 */
	public static RunBuilder failed(Task task, String runnerName, String runnerInfo,
		Instant startTime, Instant stopTime, String errorMessage, RunErrorType runErrorType) {

		return new RunBuilder(
			new RunId(task.getId().getId()),
			task.getAuthor(),
			runnerName,
			runnerInfo,
			startTime,
			stopTime,
			task.getSource(),
			new RunError(errorMessage, runErrorType)
		);
	}

	private final RunId runId;
	private final String author;
	private final String runnerName;
	private final String runnerInfo;
	private final Instant startTime;
	private final Instant stopTime;
	@Nullable
	private final Either<CommitSource, TarSource> source;
	private final List<NewMeasurement> measurementList;
	@Nullable
	private final RunError error;

	private RunBuilder(RunId runId, String author, String runnerName, String runnerInfo,
		Instant startTime, Instant stopTime,
		@Nullable Either<CommitSource, TarSource> source, @Nullable RunError error) {

		this.runId = runId;
		this.author = Objects.requireNonNull(author);
		this.runnerName = Objects.requireNonNull(runnerName);
		this.runnerInfo = Objects.requireNonNull(runnerInfo);
		this.startTime = Objects.requireNonNull(startTime);
		this.stopTime = Objects.requireNonNull(stopTime);
		this.source = source;
		this.error = error;
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
	public void addSuccessfulMeasurement(Dimension name, Unit unit, Interpretation interpretation,
		List<Double> values) {

		MeasurementValues measurementValues = new MeasurementValues(values);
		NewMeasurement measurement = new NewMeasurement(
			this.runId, name, unit, interpretation, measurementValues
		);

		this.measurementList.add(measurement);
	}

	/**
	 * Adds a new failed measurement to the run.
	 *
	 * @param name the name of the measurement
	 * @param unit the unit of the measurement
	 * @param interpretation how the measurement is to be interpreted
	 * @param errorMessage the error message
	 */
	public void addFailedMeasurement(Dimension name, Unit unit, Interpretation interpretation,
		String errorMessage) {

		MeasurementError measurementError = new MeasurementError(errorMessage);
		NewMeasurement measurement = new NewMeasurement(
			this.runId, name, unit, interpretation, measurementError
		);

		this.measurementList.add(measurement);
	}

	/**
	 * Builds the specified run instance.
	 *
	 * @return the created run instance
	 * @throws IllegalStateException if this run cannot be built yet.
	 */
	public NewRun build() throws IllegalStateException {
		if (error != null) {
			return new NewRun(
				runId,
				author,
				runnerName,
				runnerInfo,
				startTime,
				stopTime,
				source,
				error
			);
		} else {
			if (measurementList.isEmpty()) {
				throw new IllegalStateException("measurement list is empty for: " + this);
			}

			return new NewRun(
				runId,
				author,
				runnerName,
				runnerInfo,
				startTime,
				stopTime,
				source,
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
			", source=" + source +
			", measurementList=" + measurementList +
			", error=" + error +
			'}';
	}

}
