package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.shared.protocol.serialization.EntityTest;
import org.junit.jupiter.api.Test;

class ServerBoundPacketTest extends EntityTest {

	@Test
	void serializeCorrectly() throws JsonProcessingException {
		ServerBoundPacket packet = new ServerBoundPacket(
			ServerBoundPacketType.REQUEST_RUN, objectMapper.createArrayNode());
		JsonNode packetTree = objectMapper.readTree(objectMapper.writeValueAsString(packet));

		String expected = "{\"type\": \"request_run\", \"data\": []}";
		JsonNode expectedTree = objectMapper.readTree(expected);

		assertEquals(expectedTree, packetTree);
	}

	@Test
	void deserializeCorrectly() throws JsonProcessingException {
		String json = "{\"type\": \"get_status_reply\", \"data\": {}}";
		ServerBoundPacket result = objectMapper.readValue(json, ServerBoundPacket.class);

		assertEquals(
			new ServerBoundPacket(ServerBoundPacketType.GET_STATUS_REPLY,
				objectMapper.createObjectNode()),
			result
		);
	}
}