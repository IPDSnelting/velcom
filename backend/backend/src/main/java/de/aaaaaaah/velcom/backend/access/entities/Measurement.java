package de.aaaaaaah.velcom.backend.access.entities;

import de.aaaaaaah.velcom.shared.util.Either;
import java.util.Objects;

/**
 * A measurement can either be successful, in which case it must contain the measured values and
 * additional information regarding those values, or it can be failed, in which case it must contain
 * an error message.
 */
public class Measurement {

	private final RunId runId;
	private final Dimension dimension;
	private final Either<MeasurementError, MeasurementValues> content;

	public Measurement(RunId runId, Dimension dimension, MeasurementError error) {
		this.runId = Objects.requireNonNull(runId);
		this.dimension = Objects.requireNonNull(dimension);
		this.content = Either.ofLeft(error);
	}

	public Measurement(RunId runId, Dimension dimension, MeasurementValues values) {
		this.runId = Objects.requireNonNull(runId);
		this.dimension = Objects.requireNonNull(dimension);
		this.content = Either.ofRight(values);
	}

	public RunId getRunId() {
		return runId;
	}

	public Dimension getDimension() {
		return dimension;
	}

	public Either<MeasurementError, MeasurementValues> getContent() {
		return content;
	}

}
