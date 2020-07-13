package de.aaaaaaah.velcom.backend.access.entities;

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
	private final Unit unit;
	private final Interpretation interpretation;
	private final Either<MeasurementError, MeasurementValues> content;

	public Measurement(RunId runId, MeasurementName measurementName, Unit unit,
		Interpretation interpretation, MeasurementError error) {
		this.runId = Objects.requireNonNull(runId);
		this.measurementName = Objects.requireNonNull(measurementName);
		this.unit = Objects.requireNonNull(unit);
		this.interpretation = Objects.requireNonNull(interpretation);
		this.content = Either.ofLeft(error);
	}

	public Measurement(RunId runId, MeasurementName measurementName, Unit unit,
		Interpretation interpretation, MeasurementValues values) {
		this.runId = Objects.requireNonNull(runId);
		this.measurementName = Objects.requireNonNull(measurementName);
		this.unit = Objects.requireNonNull(unit);
		this.interpretation = Objects.requireNonNull(interpretation);
		this.content = Either.ofRight(values);
	}

	public RunId getRunId() {
		return runId;
	}

	public MeasurementName getMeasurementName() {
		return measurementName;
	}

	public Unit getUnit() {
		return unit;
	}

	public Interpretation getInterpretation() {
		return interpretation;
	}

	public Either<MeasurementError, MeasurementValues> getContent() {
		return content;
	}

}
