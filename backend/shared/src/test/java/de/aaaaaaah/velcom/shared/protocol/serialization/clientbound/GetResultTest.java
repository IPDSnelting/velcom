package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.shared.protocol.serialization.EntityTest;
import org.junit.jupiter.api.Test;

class GetResultTest extends EntityTest {

	@Test
	void serializeToEmptyObject() throws JsonProcessingException {
		String result = objectMapper.writeValueAsString(new GetResult());
		JsonNode tree = objectMapper.readTree(result);
		assertEquals(objectMapper.createObjectNode(), tree);
	}

	@Test
	void deserializeFromEmptyObject() throws JsonProcessingException {
		objectMapper.readValue("{}", GetResult.class);
	}
}
