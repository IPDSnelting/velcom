package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.backend.access.entities.Interpretation;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonResultTest extends SerializingTest {

	@Test
	void serializeSuccessful() throws JsonProcessingException {
		Object object = JsonResult.successful(
			List.of(JsonMeasurement.failed(
				new JsonDimension("b", "m", "u", Interpretation.NEUTRAL),
				"errorText"
			))
		);
		String json = "{"
			+ "\"measurements\": [{"
			+ "  \"dimension\": {"
			+ "    \"benchmark\": \"b\","
			+ "    \"metric\": \"m\","
			+ "    \"unit\": \"u\","
			+ "    \"interpretation\": \"NEUTRAL\""
			+ "  },"
			+ "  \"error\": \"errorText\""
			+ "}]"
			+ "}";
		serializedEquals(object, json);
	}

	@Test
	void serializeBenchError() throws JsonProcessingException {
		Object object = JsonResult.benchError("benchErrorText");
		String json = "{"
			+ "\"bench_error\": \"benchErrorText\""
			+ "}";
		serializedEquals(object, json);
	}

	@Test
	void serializeVelcomError() throws JsonProcessingException {
		Object object = JsonResult.velcomError("velcomErrorText");
		String json = "{"
			+ "\"velcom_error\": \"velcomErrorText\""
			+ "}";
		serializedEquals(object, json);
	}
}
