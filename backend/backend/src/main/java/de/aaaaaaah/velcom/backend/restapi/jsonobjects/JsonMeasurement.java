package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.util.Either;
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

	public static JsonMeasurement successful(JsonDimension dimension, double value) {

		return new JsonMeasurement(dimension, value, null, null);
	}

	public static JsonMeasurement failed(JsonDimension dimension, String error) {
		return new JsonMeasurement(dimension, null, null, error);
	}

	/**
	 * Create a {@link JsonMeasurement} from a {@link Measurement}.
	 *
	 * @param measurement the {@link Measurement} to use
	 * @param allValues whether the full lists of values should also be included
	 * @return the newly created {@link JsonMeasurement}
	 */
	public static JsonMeasurement fromMeasurement(Measurement measurement, DimensionInfo dimensionInfo, boolean allValues) {

		if (!measurement.getDimension().equals(dimensionInfo.getDimension())) {
			throw new IllegalArgumentException("measurement must have same dimension as dimension info");
		}

		JsonDimension dimension = JsonDimension.fromDimension(dimensionInfo);

		Either<MeasurementError, MeasurementValues> content = measurement.getContent();
		if (content.isLeft()) {
			MeasurementError left = content.getLeft().get();
			return failed(dimension, left.getErrorMessage());
		} else {
			MeasurementValues right = content.getRight().get();
			if (allValues) {
				return successful(dimension, right.getAverageValue(), right.getValues());
			} else {
				return successful(dimension, right.getAverageValue());
			}
		}
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
