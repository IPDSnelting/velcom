package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.backend.restapi.SerializingTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JsonRunTest extends SerializingTest {

	@Test
	void serialize() throws JsonProcessingException {
		Object object = new JsonRun(
			UUID.fromString("24dd4fd3-5c6d-4542-a7a4-b181f37295a6"),
			"authorName",
			"runnerName",
			"runnerInfoText",
			1596881630,
			1596881676,
			JsonSource.fromUploadedTar("descriptionText", null),
			JsonResult.velcomError("velcomErrorText")
		);
		String json = "{"
			+ "\"id\": \"24dd4fd3-5c6d-4542-a7a4-b181f37295a6\","
			+ "\"author\": \"authorName\","
			+ "\"runner_name\": \"runnerName\","
			+ "\"runner_info\": \"runnerInfoText\","
			+ "\"start_time\": 1596881630,"
			+ "\"stop_time\": 1596881676,"
			+ "\"source\": {"
			+ "  \"type\": \"UPLOADED_TAR\","
			+ "  \"source\": {\"description\": \"descriptionText\"}"
			+ "},"
			+ "\"result\": {\"velcom_error\": \"velcomErrorText\"}"
			+ "}";
		serializedEquals(object, json);
	}
}
