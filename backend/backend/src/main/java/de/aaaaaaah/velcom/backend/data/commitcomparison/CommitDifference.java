package de.aaaaaaah.velcom.backend.data.commitcomparison;

import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;

/**
 * This class represents a difference in a specific measurement between two commits.
 */
public class CommitDifference {

	private final MeasurementName measurementName;
	private final double difference;

	public CommitDifference(MeasurementName measurementName, double difference) {
		this.measurementName = measurementName;
		this.difference = difference;
	}

	public MeasurementName getMeasurementName() {
		return measurementName;
	}

	public double getDifference() {
		return difference;
	}

}
