package de.aaaaaaah.velcom.backend.restapi.newjsonobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.backend.access.entities.Interpretation;
import de.aaaaaaah.velcom.backend.restapi.SerializingTest;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonMeasurementTest extends SerializingTest {

	@Test
	void serializeSuccessful() throws JsonProcessingException {
		Object object = JsonMeasurement.successful(
			new JsonDimension("b", "m", "u", Interpretation.NEUTRAL),
			1.0,
			List.of(2.0, 3.0)
		);
		String json = "{"
			+ "\"dimension\": {"
			+ "  \"benchmark\": \"b\","
			+ "  \"metric\": \"m\","
			+ "  \"unit\": \"u\","
			+ "  \"interpretation\": \"NEUTRAL\""
			+ "},"
			+ "\"value\": 1.0,"
			+ "\"values\": [2.0, 3.0]"
			+ "}";
		serializedEquals(object, json);
	}

	@Test
	void serializeFailed() throws JsonProcessingException {
		Object object = JsonMeasurement.failed(
			new JsonDimension("b", "m", "u", Interpretation.NEUTRAL),
			"errorText"
		);
		String json = "{"
			+ "\"dimension\": {"
			+ "  \"benchmark\": \"b\","
			+ "  \"metric\": \"m\","
			+ "  \"unit\": \"u\","
			+ "  \"interpretation\": \"NEUTRAL\""
			+ "},"
			+ "\"error\": \"errorText\""
			+ "}";
		serializedEquals(object, json);
	}
}
