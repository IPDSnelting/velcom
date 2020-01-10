package de.aaaaaaah.designproto.backend.access.benchmark;

import de.aaaaaaah.designproto.backend.util.Either;

/**
 * A measurement can either be successful, in which case it must contain the measured values and
 * additional information regarding those values, or it can be failed, in which case it must contain
 * an error message.
 */
public class Measurement {

	private final BenchmarkAccess benchmarkAccess;

	private final RunId runId;
	private final MeasurementName measurementName;
	private final Either<MeasurementError, MeasurementValues> content;

	public Measurement(BenchmarkAccess benchmarkAccess, RunId runId,
		MeasurementName measurementName, Either<MeasurementError, MeasurementValues> content) {

		this.benchmarkAccess = benchmarkAccess;

		this.runId = runId;
		this.measurementName = measurementName;
		this.content = content;
	}

	public RunId getRunId() {
		return runId;
	}

	public Run getRun() {
		return benchmarkAccess.getRun(runId);
	}

	public MeasurementName getMeasurementName() {
		return measurementName;
	}

	public Either<MeasurementError, MeasurementValues> getContent() {
		return content;
	}
}