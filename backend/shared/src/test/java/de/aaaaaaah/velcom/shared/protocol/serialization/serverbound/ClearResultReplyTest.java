package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.shared.protocol.serialization.SerializerBasedTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClearResultReplyTest extends SerializerBasedTest {

	@Test
	void serializeToEmptyObject() {
		JsonNode tree = serializer.serializeTree(new ClearResultReply());
		assertEquals(objectMapper.createObjectNode(), tree);
	}

	@Test
	void deserializeFromEmptyObject() {
		Optional<ClearResultReply> result = serializer.deserialize("{}", ClearResultReply.class);
		assertTrue(result.isPresent());
		assertEquals(new ClearResultReply(), result.get());
	}
}
