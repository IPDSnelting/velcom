package de.aaaaaaah.velcom.runner.shared.protocol.serialization.serverbound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.EntityTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetStatusReplyTest extends EntityTest {

	@Test
	void deserializeWithNoOptionals() throws JsonProcessingException {
		String json = "{\"name\": \"TestRunner\", \"info\": \"system info goes here\", \"result_available\": false, \"state\": \"IDLE\"}";
		GetStatusReply result = objectMapper.readValue(json, GetStatusReply.class);
		assertEquals(
			new GetStatusReply("TestRunner", "system info goes here", null, false, "IDLE", null),
			result
		);
	}

	@Test
	void deserializeWithAllOptionals() throws JsonProcessingException {
		String json = "{\"name\": \"TestRunner\", \"info\": \"system info goes here\", \"bench_hash\": \"blabla\", \"result_available\": false, \"state\": \"IDLE\", \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\"}";
		GetStatusReply result = objectMapper.readValue(json, GetStatusReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertEquals(
			new GetStatusReply("TestRunner", "system info goes here", "blabla", false, "IDLE",
				uuid),
			result
		);
	}
}
