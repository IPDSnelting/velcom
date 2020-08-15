package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import javax.annotation.Nullable;

public class JsonDimensionDifference {

	private final JsonDimension dimension;
	private final double absdiff;
	@Nullable
	private final Double reldiff;
	private final double stddev;

	public JsonDimensionDifference(JsonDimension dimension, double absdiff, @Nullable Double reldiff,
		double stddev) {

		this.dimension = dimension;
		this.absdiff = absdiff;
		this.reldiff = reldiff;
		this.stddev = stddev;
	}

	public JsonDimension getDimension() {
		return dimension;
	}

	public double getAbsdiff() {
		return absdiff;
	}

	@Nullable
	public Double getReldiff() {
		return reldiff;
	}

	public double getStddev() {
		return stddev;
	}
}
