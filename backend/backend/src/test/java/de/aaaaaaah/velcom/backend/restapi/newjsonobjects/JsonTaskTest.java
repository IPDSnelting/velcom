package de.aaaaaaah.velcom.backend.restapi.newjsonobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.backend.restapi.SerializingTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JsonTaskTest extends SerializingTest {

	@Test
	void serialize() throws JsonProcessingException {
		Object object = new JsonTask(
			UUID.fromString("24dd4fd3-5c6d-4542-a7a4-b181f37295a6"),
			"authorName",
			1596881630,
			JsonSource.fromUploadedTar("descriptionText", null)
		);
		String json = "{"
			+ "\"id\": \"24dd4fd3-5c6d-4542-a7a4-b181f37295a6\","
			+ "\"author\": \"authorName\","
			+ "\"since\": 1596881630,"
			+ "\"source\": {"
			+ "  \"type\": \"UPLOADED_TAR\","
			+ "  \"source\": {\"description\": \"descriptionText\"}"
			+ "}"
			+ "}";
		serializedEquals(object, json);
	}
}
