package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;

public class JsonDimensionId {

	private final String benchmark;
	private final String metric;

	public JsonDimensionId(String benchmark, String metric) {
		this.benchmark = benchmark;
		this.metric = metric;
	}

	public String getBenchmark() {
		return benchmark;
	}

	public String getMetric() {
		return metric;
	}

	public Dimension toDimension() {
		return new Dimension(getBenchmark(), getMetric());
	}
}
