package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.util.Either;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A helper class for serialization representing a measurement.
 */
public class JsonMeasurement {

	private final String benchmark;
	private final String metric;
	@Nullable
	private final String unit;
	@Nullable
	private final String interpretation;
	@Nullable
	private final List<Double> values;
	@Nullable
	private final Double value;
	@Nullable
	private final String errorMessage;

	public JsonMeasurement(Measurement measurement) {
		benchmark = measurement.getMeasurementName().getBenchmark();
		metric = measurement.getMeasurementName().getMetric();

		Either<MeasurementError, MeasurementValues> content = measurement.getContent();
		if (content.isRight()) {
			//noinspection OptionalGetWithoutIsPresent
			MeasurementValues measurementValues = content.getRight().get();
			unit = measurement.getUnit().getName();
			interpretation = measurement.getInterpretation().getTextualRepresentation();
			values = measurementValues.getValues();
			value = measurementValues.getAverageValue();
			errorMessage = null;
		} else {
			//noinspection OptionalGetWithoutIsPresent
			MeasurementError measurementError = content.getLeft().get();
			unit = null;
			interpretation = null;
			values = null;
			value = null;
			errorMessage = measurementError.getErrorMessage();
		}
	}

	public String getBenchmark() {
		return benchmark;
	}

	public String getMetric() {
		return metric;
	}

	@Nullable
	public String getUnit() {
		return unit;
	}

	@Nullable
	public String getInterpretation() {
		return interpretation;
	}

	@Nullable
	public List<Double> getValues() {
		return values;
	}

	@Nullable
	public Double getValue() {
		return value;
	}

	@Nullable
	public String getErrorMessage() {
		return errorMessage;
	}
}
