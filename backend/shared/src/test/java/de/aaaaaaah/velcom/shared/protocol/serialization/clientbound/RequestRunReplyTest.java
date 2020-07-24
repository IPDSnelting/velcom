package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.shared.protocol.serialization.SerializerBasedTest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestRunReplyTest extends SerializerBasedTest {

	@Test
	void deserializeWithNoOptionals() throws JsonProcessingException {
		String json = "{\"bench\": false, \"run\": false}";
		Optional<RequestRunReply> result = serializer.deserialize(json, RequestRunReply.class);

		assertTrue(result.isPresent());
		assertEquals(
			new RequestRunReply(false, null, false, null),
			result.get()
		);
	}

	@Test
	void deserializeWithAllOptionals() throws JsonProcessingException {
		String json = "{\"bench\": true, \"bench_hash\": \"yay, I'm a hash\", \"run\": true, \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\"}";
		Optional<RequestRunReply> result = serializer.deserialize(json, RequestRunReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertTrue(result.isPresent());
		assertEquals(
			new RequestRunReply(true, "yay, I'm a hash", true, uuid),
			result.get()
		);
	}

}
