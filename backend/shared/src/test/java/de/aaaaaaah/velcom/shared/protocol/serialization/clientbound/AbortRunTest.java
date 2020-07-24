package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.shared.protocol.serialization.SerializerBasedTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AbortRunTest extends SerializerBasedTest {

	@Test
	void serializeToEmptyObject() {
		JsonNode tree = serializer.serializeTree(new AbortRun());
		assertEquals(objectMapper.createObjectNode(), tree);
	}

	@Test
	void deserializeFromEmptyObject() {
		Optional<AbortRun> result = serializer.deserialize("{}", AbortRun.class);
		assertTrue(result.isPresent());
		assertEquals(new AbortRun(), result.get());
	}

}