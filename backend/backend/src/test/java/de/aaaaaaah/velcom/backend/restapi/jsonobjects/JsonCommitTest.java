package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.backend.restapi.SerializingTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JsonCommitTest extends SerializingTest {

	@Test
	void serialize() throws JsonProcessingException {
		Object object = new JsonCommit(
			UUID.fromString("24dd4fd3-5c6d-4542-a7a4-b181f37295a6"),
			"e16272feb472dc4d357cc19dd97112c036a67990",
			List.of(),
			"authorName",
			1596881630,
			"committerName",
			1596881676,
			"summaryText",
			null,
			List.of()
		);
		String json = "{"
			+ "\"repo_id\": \"24dd4fd3-5c6d-4542-a7a4-b181f37295a6\","
			+ "\"hash\": \"e16272feb472dc4d357cc19dd97112c036a67990\","
			+ "\"parents\": [],"
			+ "\"author\": \"authorName\","
			+ "\"author_date\": 1596881630,"
			+ "\"committer\": \"committerName\","
			+ "\"committer_date\": 1596881676,"
			+ "\"summary\": \"summaryText\","
			+ "\"runs\": []"
			+ "}";
		serializedEquals(object, json);
	}
}
