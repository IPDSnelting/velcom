package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.shared.protocol.serialization.SerializerBasedTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClearResultTest extends SerializerBasedTest {

	@Test
	void serializeToEmptyObject() {
		JsonNode tree = serializer.serializeTree(new ClearResult());
		assertEquals(objectMapper.createObjectNode(), tree);
	}

	@Test
	void deserializeFromEmptyObject() {
		Optional<ClearResult> result = serializer.deserialize("{}", ClearResult.class);
		assertTrue(result.isPresent());
		assertEquals(new ClearResult(), result.get());
	}
}
