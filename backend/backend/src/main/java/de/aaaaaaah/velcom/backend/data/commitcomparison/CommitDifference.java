package de.aaaaaaah.velcom.backend.data.commitcomparison;

import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementName;

/**
 * This class represents a difference in a specific measurement between two commits.
 */
public class CommitDifference {

	private final MeasurementName measurementName;
	private final double first;
	private final double second;

	CommitDifference(MeasurementName measurementName, double first, double second) {
		this.measurementName = measurementName;
		this.first = first;
		this.second = second;
	}

	public MeasurementName getMeasurementName() {
		return measurementName;
	}

	public double getFirst() {
		return first;
	}

	public double getSecond() {
		return second;
	}

	public double getDifference() {
		return second - first;
	}

}
