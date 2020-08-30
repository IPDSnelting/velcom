package de.aaaaaaah.velcom.backend.data.commitcomparison;

import de.aaaaaaah.velcom.backend.access.entities.Dimension;

/**
 * This class represents a difference in a specific measurement between two commits.
 */
public class CommitDifference {

	private final Dimension dimension;
	private final double first;
	private final double second;

	CommitDifference(Dimension dimension, double first, double second) {
		this.dimension = dimension;
		this.first = first;
		this.second = second;
	}

	public Dimension getMeasurementName() {
		return dimension;
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
