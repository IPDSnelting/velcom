package de.aaaaaaah.velcom.backend.newaccess.entities;

import de.aaaaaaah.velcom.backend.util.Either;

import java.util.Objects;

/**
 * A measurement can either be successful, in which case it must contain the measured values and
 * additional information regarding those values, or it can be failed, in which case it must contain
 * an error message.
 */
public class Measurement {

	private final RunId runId;
	private final MeasurementName measurementName;
	private final Either<MeasurementError, MeasurementValues> content;

	public Measurement(RunId runId, MeasurementName measurementName, MeasurementError error) {
		this.runId = Objects.requireNonNull(runId);
		this.measurementName = Objects.requireNonNull(measurementName);
		this.content = Either.ofLeft(error);
	}

	public Measurement(RunId runId, MeasurementName measurementName, MeasurementValues values) {
		this.runId = Objects.requireNonNull(runId);
		this.measurementName = Objects.requireNonNull(measurementName);
		this.content = Either.ofRight(values);
	}

	public RunId getRunId() {
		return runId;
	}

	public MeasurementName getMeasurementName() {
		return measurementName;
	}

	public Either<MeasurementError, MeasurementValues> getContent() {
		return content;
	}

}
