package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.aaaaaaah.velcom.shared.protocol.serialization.SerializerBasedTest;
import de.aaaaaaah.velcom.shared.protocol.serialization.Status;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetStatusReplyTest extends SerializerBasedTest {

	@Test
	void deserializeWithNoOptionals() {
		String json = "{\"info\": \"system info goes here\", \"result_available\": false, \"status\": \"IDLE\"}";
		Optional<GetStatusReply> result = serializer.deserialize(json, GetStatusReply.class);

		assertTrue(result.isPresent());
		assertEquals(
			new GetStatusReply("system info goes here", null, false, Status.IDLE, null),
			result.get()
		);
	}

	@Test
	void deserializeWithAllOptionals() {
		String json = "{\"info\": \"system info goes here\", \"bench_hash\": \"blabla\", \"result_available\": false, \"status\": \"ABORT\", \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\"}";
		Optional<GetStatusReply> result = serializer.deserialize(json, GetStatusReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertTrue(result.isPresent());
		assertEquals(
			new GetStatusReply("system info goes here", "blabla", false, Status.ABORT, uuid),
			result.get()
		);
	}
}