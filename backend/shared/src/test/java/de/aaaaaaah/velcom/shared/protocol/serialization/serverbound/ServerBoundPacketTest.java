package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.shared.protocol.serialization.SerializerBasedTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServerBoundPacketTest extends SerializerBasedTest {

	@Test
	void serializeCorrectly() throws JsonProcessingException {
		ServerBoundPacket packet = new ServerBoundPacket(
			ServerBoundPacketType.REQUEST_RUN,
			objectMapper.createArrayNode()
		);
		JsonNode packetTree = serializer.serializeTree(packet);

		String expected = "{\"type\": \"request_run\", \"data\": []}";
		JsonNode expectedTree = objectMapper.readTree(expected);

		assertEquals(expectedTree, packetTree);
	}

	@Test
	void deserializeCorrectly() throws JsonProcessingException {
		String json = "{\"type\": \"get_status_reply\", \"data\": {}}";
		Optional<ServerBoundPacket> result = serializer.deserialize(json,
			ServerBoundPacket.class);

		assertTrue(result.isPresent());
		assertEquals(
			new ServerBoundPacket(
				ServerBoundPacketType.GET_STATUS_REPLY,
				objectMapper.createObjectNode()
			),
			result.get()
		);
	}
}