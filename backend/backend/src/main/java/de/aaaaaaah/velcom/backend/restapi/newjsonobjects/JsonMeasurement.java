package de.aaaaaaah.velcom.backend.restapi.newjsonobjects;

import java.util.List;
import javax.annotation.Nullable;

public class JsonMeasurement {

	private final JsonDimension dimension;
	@Nullable
	private final Double value;
	@Nullable
	private final List<Double> values;
	@Nullable
	private final String error;

	private JsonMeasurement(JsonDimension dimension, @Nullable Double value,
		@Nullable List<Double> values, @Nullable String error) {
		this.dimension = dimension;
		this.value = value;
		this.values = values;
		this.error = error;
	}

	public static JsonMeasurement successful(JsonDimension dimension, double value,
		List<Double> values) {

		return new JsonMeasurement(dimension, value, values, null);
	}

	public static JsonMeasurement failed(JsonDimension dimension, String error) {
		return new JsonMeasurement(dimension, null, null, error);
	}

	public JsonDimension getDimension() {
		return dimension;
	}

	@Nullable
	public Double getValue() {
		return value;
	}

	@Nullable
	public List<Double> getValues() {
		return values;
	}

	@Nullable
	public String getError() {
		return error;
	}
}
