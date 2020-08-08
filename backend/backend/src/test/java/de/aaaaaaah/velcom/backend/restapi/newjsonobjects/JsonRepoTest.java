package de.aaaaaaah.velcom.backend.restapi.newjsonobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.backend.access.entities.Interpretation;
import de.aaaaaaah.velcom.backend.restapi.SerializingTest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JsonRepoTest extends SerializingTest {

	@Test
	void serialize() throws JsonProcessingException {
		Object object = new JsonRepo(
			UUID.fromString("24dd4fd3-5c6d-4542-a7a4-b181f37295a6"),
			"velcom",
			URI.create("https://vel.com/"),
			List.of("untracked", "branches"),
			List.of("main"),
			false,
			List.of(new JsonDimension("b", "m", "u", Interpretation.NEUTRAL))
		);
		String json = "{"
			+ "\"id\": \"24dd4fd3-5c6d-4542-a7a4-b181f37295a6\","
			+ "\"name\": \"velcom\","
			+ "\"remote_url\": \"https://vel.com/\","
			+ "\"untracked_branches\": [\"untracked\", \"branches\"],"
			+ "\"tracked_branches\": [\"main\"],"
			+ "\"has_token\": false,"
			+ "\"dimensions\": [{"
			+ "  \"benchmark\": \"b\","
			+ "  \"metric\": \"m\","
			+ "  \"unit\": \"u\","
			+ "  \"interpretation\": \"NEUTRAL\""
			+ "}]"
			+ "}";
		serializedEquals(object, json);
	}
}
