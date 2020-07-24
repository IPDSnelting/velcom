package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.shared.protocol.serialization.SerializerBasedTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClientBoundPacketTest extends SerializerBasedTest {

	@Test
	void serializeCorrectly() throws JsonProcessingException {
		ClientBoundPacket packet = new ClientBoundPacket(
			ClientBoundPacketType.CLEAR_RESULT,
			objectMapper.createArrayNode()
		);
		JsonNode packetTree = serializer.serializeTree(packet);

		String expected = "{\"type\": \"clear_result\", \"data\": []}";
		JsonNode expectedTree = objectMapper.readTree(expected);

		assertEquals(expectedTree, packetTree);
	}

	@Test
	void deserializeCorrectly() {
		String json = "{\"type\": \"request_run_reply\", \"data\": {}}";
		Optional<ClientBoundPacket> result = serializer.deserialize(json,
			ClientBoundPacket.class);

		assertTrue(result.isPresent());
		assertEquals(
			new ClientBoundPacket(
				ClientBoundPacketType.REQUEST_RUN_REPLY,
				objectMapper.createObjectNode()
			),
			result.get()
		);
	}
}