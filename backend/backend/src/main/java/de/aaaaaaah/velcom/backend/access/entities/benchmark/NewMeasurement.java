package de.aaaaaaah.velcom.backend.access.entities.benchmark;

import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.shared.util.Either;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * An entity used for adding new runs via the BenchmarkAccess.
 */
public class NewMeasurement {

	private final RunId runId;
	private final Dimension dimension;
	@Nullable
	private final Unit unit;
	@Nullable
	private final Interpretation interpretation;
	private final Either<MeasurementError, MeasurementValues> content;

	public NewMeasurement(RunId runId, Dimension dimension, @Nullable Unit unit,
		@Nullable Interpretation interpretation, MeasurementError error) {
		this(runId, dimension, unit, interpretation, Either.ofLeft(error));
	}

	public NewMeasurement(RunId runId, Dimension dimension, @Nullable Unit unit,
		@Nullable Interpretation interpretation, MeasurementValues values) {
		this(runId, dimension, unit, interpretation, Either.ofRight(values));
	}

	private NewMeasurement(RunId runId, Dimension dimension, @Nullable Unit unit,
		@Nullable Interpretation interpretation, Either<MeasurementError, MeasurementValues> content) {

		this.runId = runId;
		this.dimension = dimension;
		this.unit = unit;
		this.interpretation = interpretation;
		this.content = content;
	}

	public RunId getRunId() {
		return runId;
	}

	public Dimension getDimension() {
		return dimension;
	}

	public Optional<Unit> getUnit() {
		return Optional.ofNullable(unit);
	}

	public Optional<Interpretation> getInterpretation() {
		return Optional.ofNullable(interpretation);
	}

	public Either<MeasurementError, MeasurementValues> getContent() {
		return content;
	}

	public Measurement toMeasurement() {
		return content.consume(
			error -> new Measurement(runId, dimension, error),
			values -> new Measurement(runId, dimension, values)
		);
	}

	@Override
	public String toString() {
		return "NewMeasurement{" +
			"runId=" + runId +
			", dimension=" + dimension +
			", unit=" + unit +
			", interpretation=" + interpretation +
			", content=" + content +
			'}';
	}
}
