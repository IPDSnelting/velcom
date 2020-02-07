package de.aaaaaaah.velcom.backend.newaccess.entities;

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
	 * @param repoId     the id of the repository
	 * @param commitHash the hash of the commit
	 * @param startTime  the start time
	 * @param stopTime   the stop time
	 * @return a new builder instance
	 */
	public static RunBuilder successful(RepoId repoId, CommitHash commitHash, Instant startTime, Instant stopTime) {
		return new RunBuilder(repoId, commitHash, startTime, stopTime, null);
	}

	/**
	 * Creates a new run builder for failed runs.
	 *
	 * @param repoId       the id of the repository
	 * @param commitHash   the hash of the commit
	 * @param startTime    the start time
	 * @param stopTime     the stop time
	 * @param errorMessage the error message
	 * @return a new builder instance
	 */
	public static RunBuilder failed(RepoId repoId, CommitHash commitHash, Instant startTime, Instant stopTime,
	                                String errorMessage) {
		return new RunBuilder(repoId, commitHash, startTime, stopTime, errorMessage);
	}

	private final RunId runId;
	private final RepoId repoId;
	private final CommitHash commitHash;
	private final Instant startTime;
	private final Instant stopTime;

	@Nullable
	private final String errorMessage;

	private final List<Measurement> measurementList;

	private RunBuilder(RepoId repoId, CommitHash commitHash, Instant startTime, Instant stopTime,
	                   @Nullable String errorMessage) {

		this.runId = new RunId();
		this.repoId = Objects.requireNonNull(repoId);
		this.commitHash = Objects.requireNonNull(commitHash);
		this.startTime = Objects.requireNonNull(startTime);
		this.stopTime = Objects.requireNonNull(stopTime);
		this.errorMessage = errorMessage;
		this.measurementList = errorMessage != null ? Collections.emptyList() : new ArrayList<>();
	}

	/**
	 * Adds a new successful measurement to the run.
	 *
	 * @param name           the name of the measurement
	 * @param interpretation the interpretation
	 * @param unit           the unit
	 * @param values         the values
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
	 * @param name         the name of the measurement
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
	 */
	public Run build() {
		if (this.errorMessage != null) {
			return new Run(
				runId,
				repoId,
				commitHash,
				startTime,
				stopTime,
				errorMessage
			);
		} else {
			if (this.measurementList.isEmpty()) {
				throw new IllegalStateException("measurement list is empty for: " + this);
			}

			return new Run(
				runId,
				repoId,
				commitHash,
				startTime,
				stopTime,
				measurementList
			);
		}
	}

	@Override
	public String toString() {
		return "RunBuilder{" +
			"runId=" + runId +
			", repoId=" + repoId +
			", commitHash=" + commitHash +
			", startTime=" + startTime +
			", stopTime=" + stopTime +
			", errorMessage='" + errorMessage + '\'' +
			", measurementList=" + measurementList +
			'}';
	}

}
