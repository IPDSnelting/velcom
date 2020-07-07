package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.shared.protocol.serialization.EntityTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestRunReplyTest extends EntityTest {

	@Test
	void deserializeWithNoOptionals() throws JsonProcessingException {
		String json = "{\"bench\": false, \"run\": false}";
		RequestRunReply result = objectMapper.readValue(json, RequestRunReply.class);
		assertEquals(
			new RequestRunReply(false, null, false, null),
			result
		);
	}

	@Test
	void deserializeWithAllOptionals() throws JsonProcessingException {
		String json = "{\"bench\": true, \"bench_hash\": \"yay, I'm a hash\", \"run\": true, \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\"}";
		RequestRunReply result = objectMapper.readValue(json, RequestRunReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertEquals(
			new RequestRunReply(true, "yay, I'm a hash", true, uuid),
			result
		);
	}

}
