package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Interpretation;
import org.junit.jupiter.api.Test;

class JsonDimensionInfoTest extends SerializingTest {

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
