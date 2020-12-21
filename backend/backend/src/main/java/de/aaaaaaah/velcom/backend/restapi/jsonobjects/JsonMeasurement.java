package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.data.runcomparison.SignificanceFactors;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.DimensionInfo;
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
	@Nullable
	private final Double stddev;
	@Nullable
	private final Double stddevPercent;

	private JsonMeasurement(JsonDimension dimension, @Nullable Double value,
		@Nullable List<Double> values, @Nullable String error, @Nullable Double stddev,
		@Nullable Double stddevPercent) {

		this.dimension = dimension;
		this.value = value;
		this.values = values;
		this.error = error;
		this.stddev = stddev;
		this.stddevPercent = stddevPercent;
	}

	public static JsonMeasurement successful(JsonDimension dimension, double value,
		List<Double> values, Double stddev, Double stddevPercent) {
		return new JsonMeasurement(dimension, value, values, null, stddev, stddevPercent);
	}

	public static JsonMeasurement successful(JsonDimension dimension, double value) {
		return new JsonMeasurement(dimension, value, null, null, null, null);
	}

	public static JsonMeasurement failed(JsonDimension dimension, String error) {
		return new JsonMeasurement(dimension, null, null, error, null, null);
	}

	/**
	 * Create a {@link JsonMeasurement} from a {@link Measurement}.
	 *
	 * @param measurement the {@link Measurement} to use
	 * @param dimensionInfo the full info for the measurement's dimension
	 * @param significanceFactors the current significance factors (required for stddev
	 * 	calculations)
	 * @param allValues whether the full lists of values should also be included
	 * @return the newly created {@link JsonMeasurement}
	 */
	public static JsonMeasurement fromMeasurement(Measurement measurement,
		DimensionInfo dimensionInfo, SignificanceFactors significanceFactors, boolean allValues) {

		if (!measurement.getDimension().equals(dimensionInfo.getDimension())) {
			throw new IllegalArgumentException("measurement must have same dimension as dimension info");
		}

		JsonDimension dimension = JsonDimension.fromDimensionInfo(dimensionInfo);

		return measurement.getContent().consume(
			left -> failed(dimension, left.getErrorMessage()),
			right -> successful(
				dimension,
				right.getAverageValue(),
				allValues ? right.getValues() : null,
				right.getStddevWith(significanceFactors).orElse(null),
				right.getStddevPercentWith(significanceFactors).orElse(null)
			)
		);
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

	@Nullable
	public Double getStddev() {
		return stddev;
	}

	@Nullable
	public Double getStddevPercent() {
		return stddevPercent;
	}
}
