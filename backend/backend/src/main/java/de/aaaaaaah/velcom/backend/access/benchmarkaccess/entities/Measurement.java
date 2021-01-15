package de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities;

import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Measurement that = (Measurement) o;
		return Objects.equals(runId, that.runId) && Objects
			.equals(dimension, that.dimension) && Objects.equals(content, that.content);
	}

	@Override
	public int hashCode() {
		return Objects.hash(runId, dimension, content);
	}
}
