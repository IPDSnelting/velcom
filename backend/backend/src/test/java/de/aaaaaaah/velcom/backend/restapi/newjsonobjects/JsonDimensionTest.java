package de.aaaaaaah.velcom.backend.restapi.newjsonobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.backend.access.entities.Interpretation;
import de.aaaaaaah.velcom.backend.restapi.SerializingTest;
import org.junit.jupiter.api.Test;

class JsonDimensionTest extends SerializingTest {

	@Test
	void serialize() throws JsonProcessingException {
		Object object = new JsonDimension(
			"benchmarkName",
			"metricName",
			"someUnit",
			Interpretation.LESS_IS_BETTER
		);
		String json = "{"
			+ "\"benchmark\": \"benchmarkName\","
			+ "\"metric\": \"metricName\","
			+ "\"unit\": \"someUnit\","
			+ "\"interpretation\": \"LESS_IS_BETTER\""
			+ "}";
		serializedEquals(object, json);
	}
}
