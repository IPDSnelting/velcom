package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.DimensionInfo;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class JsonResult {

	@Nullable
	private final List<JsonMeasurement> measurements;
	@Nullable
	private final String benchError;
	@Nullable
	private final String velcomError;

	private JsonResult(@Nullable List<JsonMeasurement> measurements, @Nullable String benchError,
		@Nullable String velcomError) {

		this.measurements = measurements;
		this.benchError = benchError;
		this.velcomError = velcomError;
	}

	public static JsonResult successful(List<JsonMeasurement> measurements) {
		return new JsonResult(measurements, null, null);
	}

	public static JsonResult benchError(String benchError) {
		return new JsonResult(null, benchError, null);
	}

	public static JsonResult velcomError(String velcomError) {
		return new JsonResult(null, null, velcomError);
	}

	/**
	 * Create a {@link JsonResult} from a {@link RunError}.
	 *
	 * @param error the error
	 * @return the newly created {@link JsonResult}
	 */
	public static JsonResult fromRunError(RunError error) {
		switch (error.getType()) {
			case BENCH_SCRIPT_ERROR:
				return benchError(error.getMessage());
			case VELCOM_ERROR:
				return velcomError(error.getMessage());
			default:
				// This should never happen, but java sadly doesn't understand enums well enough to know
				// that. Oh well
				return velcomError("invalid result type");
		}
	}

	/**
	 * Create a {@link JsonResult} from a list of measurements.
	 *
	 * @param measurements the measurements to use
	 * @param dimensionInfos the dimensions for each measurement
	 * @param allValues whether the full lists of values should also be included for each
	 * 	measurement
	 * @return the newly created {@link JsonResult}
	 */
	public static JsonResult fromMeasurements(Collection<Measurement> measurements,
		Map<Dimension, DimensionInfo> dimensionInfos, boolean allValues) {

		List<JsonMeasurement> jsonMeasurements = measurements.stream()
			.map(measurement -> JsonMeasurement
				.fromMeasurement(measurement, dimensionInfos.get(measurement.getDimension()), allValues))
			.collect(Collectors.toList());
		return successful(jsonMeasurements);
	}

	@Nullable
	public List<JsonMeasurement> getMeasurements() {
		return measurements;
	}

	@Nullable
	public String getBenchError() {
		return benchError;
	}

	@Nullable
	public String getVelcomError() {
		return velcomError;
	}
}
