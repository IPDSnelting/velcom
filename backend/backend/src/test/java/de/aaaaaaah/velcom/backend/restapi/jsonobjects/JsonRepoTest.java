package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JsonRepoTest extends SerializingTest {

	@Test
	void serialize() throws JsonProcessingException {
		Object object = new JsonRepo(
			UUID.fromString("24dd4fd3-5c6d-4542-a7a4-b181f37295a6"),
			"velcom",
			"https://vel.com/",
			List.of(
				new JsonBranch("main", true, "foo"),
				new JsonBranch("untracked", false, "bar"),
				new JsonBranch("branches", false, "baz")
			),
			List.of(new JsonDimension("b", "m", "u", Interpretation.NEUTRAL))
		);
		String json = "{"
			+ "\"id\": \"24dd4fd3-5c6d-4542-a7a4-b181f37295a6\","
			+ "\"name\": \"velcom\","
			+ "\"remote_url\": \"https://vel.com/\","
			+ "\"branches\": ["
			+ "  {\"name\": \"main\", \"tracked\": true, \"latest_commit\": \"foo\"},"
			+ "  {\"name\": \"untracked\", \"tracked\": false, \"latest_commit\": \"bar\"},"
			+ "  {\"name\": \"branches\", \"tracked\": false, \"latest_commit\": \"baz\"}"
			+ "],"
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
