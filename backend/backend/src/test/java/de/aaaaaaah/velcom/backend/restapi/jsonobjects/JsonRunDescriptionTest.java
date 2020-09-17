package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription.JsonSuccess;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JsonRunDescriptionTest extends SerializingTest {

	@Test
	void serialize() throws JsonProcessingException {
		Object object = new JsonRunDescription(
			UUID.fromString("24dd4fd3-5c6d-4542-a7a4-b181f37295a6"),
			1596881630,
			JsonSuccess.SUCCESS,
			JsonSource.tarSource(
				"descriptionText",
				null
			)
		);
		String json = "{"
			+ "\"id\": \"24dd4fd3-5c6d-4542-a7a4-b181f37295a6\","
			+ "\"start_time\": 1596881630,"
			+ "\"success\": \"SUCCESS\","
			+ "\"source\": {"
			+ "    \"type\": \"UPLOADED_TAR\","
			+ "    \"source\": {\"description\": \"descriptionText\"}"
			+ "}"
			+ "}";
		serializedEquals(object, json);
	}
}
