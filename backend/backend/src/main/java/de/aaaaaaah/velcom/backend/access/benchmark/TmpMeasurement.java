package de.aaaaaaah.velcom.backend.access.benchmark;

import de.aaaaaaah.velcom.backend.util.Either;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

class TmpMeasurement {

	private final RunId runId;
	private final MeasurementName measurementName;
	@Nullable
	private final Unit unit;
	@Nullable
	private final Interpretation interpretation;
	@Nullable
	private final String errorMessage;
	private final List<Double> values;

	TmpMeasurement(RunId runId, MeasurementName measurementName, @Nullable Unit unit,
		@Nullable Interpretation interpretation, @Nullable String errorMessage) {

		this.runId = runId;
		this.measurementName = measurementName;
		this.unit = unit;
		this.interpretation = interpretation;
		this.errorMessage = errorMessage;

		values = new ArrayList<>();
	}

	public RunId getRunId() {
		return runId;
	}

	public MeasurementName getMeasurementName() {
		return measurementName;
	}

	public List<Double> getValues() {
		return values;
	}

	public Measurement toMeasurement(BenchmarkAccess benchmarkAccess) {
		final Either<MeasurementError, MeasurementValues> errorOrValues;
		if (errorMessage == null) {
			errorOrValues = Either.ofRight(
				new MeasurementValues(values, unit, interpretation)
			);
		} else {
			errorOrValues = Either.ofLeft(new MeasurementError(errorMessage));
		}

		return new Measurement(benchmarkAccess, runId, measurementName, errorOrValues);
	}
}
