package de.aaaaaaah.designproto.backend.restapi.jsonobjects;

import de.aaaaaaah.designproto.backend.access.benchmark.MeasurementName;

/**
 * A helper class for serialization representing a measurement name.
 */
public class JsonMeasurementName {

	private final String benchmark;
	private final String metric;

	public JsonMeasurementName(MeasurementName measurementName) {
		benchmark = measurementName.getBenchmark();
		metric = measurementName.getMetric();
	}

	public String getBenchmark() {
		return benchmark;
	}

	public String getMetric() {
		return metric;
	}
}
