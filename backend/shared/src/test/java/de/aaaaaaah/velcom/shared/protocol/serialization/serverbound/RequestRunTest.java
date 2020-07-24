package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.shared.protocol.serialization.SerializerBasedTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RequestRunTest extends SerializerBasedTest {

	@Test
	void serializeToEmptyObject() {
		JsonNode tree = serializer.serializeTree(new RequestRun());
		assertEquals(objectMapper.createObjectNode(), tree);
	}

	@Test
	void deserializeFromEmptyObject() {
		Optional<RequestRun> result = serializer.deserialize("{}", RequestRun.class);
		assertTrue(result.isPresent());
		assertEquals(new RequestRun(), result.get());
	}
}
