package de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.EntityTest;
import org.junit.jupiter.api.Test;

class ClientBoundPacketTest extends EntityTest {

	@Test
	void serializeCorrectly() throws JsonProcessingException {
		ClientBoundPacket packet = new ClientBoundPacket(
			ClientBoundPacketType.CLEAR_RESULT, objectMapper.createArrayNode());
		JsonNode packetTree = objectMapper.readTree(objectMapper.writeValueAsString(packet));

		String expected = "{\"type\": \"clear_result\", \"data\": []}";
		JsonNode expectedTree = objectMapper.readTree(expected);

		assertEquals(expectedTree, packetTree);
	}

	@Test
	void deserializeCorrectly() throws JsonProcessingException {
		String json = "{\"type\": \"request_run_reply\", \"data\": {}}";
		ClientBoundPacket result = objectMapper.readValue(json, ClientBoundPacket.class);

		assertEquals(
			new ClientBoundPacket(ClientBoundPacketType.REQUEST_RUN_REPLY,
				objectMapper.createObjectNode()),
			result
		);
	}
}