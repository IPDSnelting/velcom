package de.aaaaaaah.designproto.backend.restapi.jsonobjects;

import de.aaaaaaah.designproto.backend.data.commitcomparison.CommitDifference;


/**
 * A helper class for serialization representing a difference in a certain measurement between two
 * commits.
 */
public class JsonDifference {

	private final String benchmark;
	private final String metric;
	private final double difference;

	public JsonDifference(CommitDifference commitDifference) {
		benchmark = commitDifference.getMeasurementName().getBenchmark();
		metric = commitDifference.getMeasurementName().getMetric();
		difference = commitDifference.getDifference();
	}

	public String getBenchmark() {
		return benchmark;
	}

	public String getMetric() {
		return metric;
	}

	public double getDifference() {
		return difference;
	}

}
