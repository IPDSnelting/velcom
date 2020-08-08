package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JsonCommitDescriptionTest extends SerializingTest {

	@Test
	void serialize() throws JsonProcessingException {
		Object object = new JsonCommitDescription(
			UUID.fromString("24dd4fd3-5c6d-4542-a7a4-b181f37295a6"),
			"e16272feb472dc4d357cc19dd97112c036a67990",
			"authorName",
			1596881630,
			"summaryText"
		);
		String json = "{"
			+ "\"repo_id\": \"24dd4fd3-5c6d-4542-a7a4-b181f37295a6\","
			+ "\"hash\": \"e16272feb472dc4d357cc19dd97112c036a67990\","
			+ "\"author\": \"authorName\","
			+ "\"author_date\": 1596881630,"
			+ "\"summary\": \"summaryText\""
			+ "}";
		serializedEquals(object, json);
	}
}
