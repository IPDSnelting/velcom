package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.backend.restapi.SerializingTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JsonSourceTest extends SerializingTest {

	@Test
	void serializeFromCommit() throws JsonProcessingException {
		Object object = JsonSource.fromCommit(new JsonCommitDescription(
			UUID.fromString("24dd4fd3-5c6d-4542-a7a4-b181f37295a6"),
			"e16272feb472dc4d357cc19dd97112c036a67990",
			"authorName",
			1596881630,
			"summaryText"
		));
		String json = "{"
			+ "\"type\": \"COMMIT\","
			+ "\"source\": {"
			+ "  \"repo_id\": \"24dd4fd3-5c6d-4542-a7a4-b181f37295a6\","
			+ "  \"hash\": \"e16272feb472dc4d357cc19dd97112c036a67990\","
			+ "  \"author\": \"authorName\","
			+ "  \"author_date\": 1596881630,"
			+ "  \"summary\": \"summaryText\""
			+ "}"
			+ "}";
		serializedEquals(object, json);
	}

	@Test
	void serializeFromUploadedTar() throws JsonProcessingException {
		Object object = JsonSource.fromUploadedTar(
			"descriptionText",
			UUID.fromString("24dd4fd3-5c6d-4542-a7a4-b181f37295a6")
		);
		String json = "{"
			+ "\"type\": \"UPLOADED_TAR\","
			+ "\"source\": {"
			+ "  \"description\": \"descriptionText\","
			+ "  \"repo_id\": \"24dd4fd3-5c6d-4542-a7a4-b181f37295a6\""
			+ "}"
			+ "}";
		serializedEquals(object, json);

		object = JsonSource.fromUploadedTar(
			"descriptionText",
			null
		);
		json = "{"
			+ "\"type\": \"UPLOADED_TAR\","
			+ "\"source\": {"
			+ "  \"description\": \"descriptionText\""
			+ "}"
			+ "}";
		serializedEquals(object, json);
	}
}
