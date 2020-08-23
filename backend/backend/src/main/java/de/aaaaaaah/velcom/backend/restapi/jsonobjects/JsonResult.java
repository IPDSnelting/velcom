package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunError;
import de.aaaaaaah.velcom.backend.util.Either;
import java.util.Collection;
import java.util.List;
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
	 * Create a {@link JsonResult} from a result returned by {@link Run#getResult()}.
	 *
	 * @param result the result to use
	 * @param allValues whether the full lists of values should also be included for each
	 * 	measurement
	 * @return the newly created {@link JsonResult}
	 */
	public static JsonResult fromRunResult(Either<RunError, Collection<Measurement>> result,
		boolean allValues) {

		if (result.isLeft()) {
			RunError left = result.getLeft().get();
			switch (left.getType()) {
				case BENCH_SCRIPT_ERROR:
					return benchError(left.getMessage());
				case VELCOM_ERROR:
					return velcomError(left.getMessage());
				default:
					// This should never happen, but java sadly doesn't understand enums well enough to know
					// that. Oh well
					return velcomError("invalid result type");
			}
		} else {
			Collection<Measurement> measurements = result.getRight().get();
			List<JsonMeasurement> jsonMeasurements = measurements.stream()
				.map(measurement -> JsonMeasurement.fromMeasurement(measurement, allValues))
				.collect(Collectors.toList());
			return successful(jsonMeasurements);
		}
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
