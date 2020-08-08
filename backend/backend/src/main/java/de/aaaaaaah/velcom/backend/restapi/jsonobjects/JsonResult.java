package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import java.util.List;
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
