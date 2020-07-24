package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.shared.protocol.serialization.SerializerBasedTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AbortRunReplyTest extends SerializerBasedTest {

	@Test
	void serializeToEmptyObject() {
		JsonNode tree = serializer.serializeTree(new AbortRunReply());
		assertEquals(objectMapper.createObjectNode(), tree);
	}

	@Test
	void deserializeFromEmptyObject() {
		Optional<AbortRunReply> result = serializer.deserialize("{}", AbortRunReply.class);
		assertTrue(result.isPresent());
		assertEquals(new AbortRunReply(), result.get());
	}

}