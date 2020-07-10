package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.shared.protocol.serialization.EntityTest;
import de.aaaaaaah.velcom.shared.protocol.serialization.Status;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetStatusReplyTest extends EntityTest {

	@Test
	void deserializeWithNoOptionals() throws JsonProcessingException {
		String json = "{\"info\": \"system info goes here\", \"result_available\": false, \"status\": \"IDLE\"}";
		GetStatusReply result = objectMapper.readValue(json, GetStatusReply.class);
		assertEquals(
			new GetStatusReply("system info goes here", null, false, Status.IDLE, null),
			result
		);
	}

	@Test
	void deserializeWithAllOptionals() throws JsonProcessingException {
		String json = "{\"info\": \"system info goes here\", \"bench_hash\": \"blabla\", \"result_available\": false, \"status\": \"ABORT\", \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\"}";
		GetStatusReply result = objectMapper.readValue(json, GetStatusReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertEquals(
			new GetStatusReply("system info goes here", "blabla", false, Status.ABORT, uuid),
			result
		);
	}
}