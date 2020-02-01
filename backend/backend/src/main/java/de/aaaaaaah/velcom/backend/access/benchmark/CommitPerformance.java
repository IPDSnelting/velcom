package de.aaaaaaah.velcom.backend.access.benchmark;

import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single commit's performance regarding a measurement.
 */
public class CommitPerformance {

	private final MeasurementName measurementName;
	private final Interpretation interpretation;
	private final Unit unit;

	private final CommitHash commitHash;
	private final List<Double> values;

	/**
	 * TODO: Documentation
	 */
	CommitPerformance(MeasurementName measurementName, Interpretation interpretation,
		Unit unit, CommitHash commitHash) {

		this.measurementName = measurementName;
		this.interpretation = interpretation;
		this.unit = unit;
		this.commitHash = commitHash;
		this.values = new ArrayList<>();
	}

	void addValue(double value) {
		this.values.add(value);
	}

	public MeasurementName getMeasurementName() {
		return measurementName;
	}

	public Interpretation getInterpretation() {
		return interpretation;
	}

	public Unit getUnit() {
		return unit;
	}

	public CommitHash getCommitHash() {
		return commitHash;
	}

	public List<Double> getValues() {
		return values;
	}

	public double getAverage() {
		return MeasurementValues.getValue(values);
	}

}
