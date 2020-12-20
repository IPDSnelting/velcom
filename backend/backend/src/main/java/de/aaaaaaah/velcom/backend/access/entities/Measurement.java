package de.aaaaaaah.velcom.backend.access.entities;

import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.shared.util.Either;

/**
 * A measurement can either be successful, in which case it must contain the measured values and
 * additional information regarding those values, or it can be failed, in which case it must contain
 * an error message.
 */
public class Measurement {

	private final RunId runId;
	private final Dimension dimension;
	private final Either<MeasurementError, MeasurementValues> content;

	public Measurement(RunId runId, Dimension dimension,
		Either<MeasurementError, MeasurementValues> content) {

		this.runId = runId;
		this.dimension = dimension;
		this.content = content;
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
