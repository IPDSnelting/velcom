package de.aaaaaaah.velcom.runner.shared.protocol.serialization.serverbound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.EntityTest;
import org.junit.jupiter.api.Test;

class ClearResultReplyTest extends EntityTest {

	@Test
	void serializeToEmptyObject() throws JsonProcessingException {
		String result = objectMapper.writeValueAsString(new ClearResultReply());
		JsonNode tree = objectMapper.readTree(result);
		assertEquals(objectMapper.createObjectNode(), tree);
	}

	@Test
	void deserializeFromEmptyObject() throws JsonProcessingException {
		objectMapper.readValue("{}", ClearResultReply.class);
	}
}
