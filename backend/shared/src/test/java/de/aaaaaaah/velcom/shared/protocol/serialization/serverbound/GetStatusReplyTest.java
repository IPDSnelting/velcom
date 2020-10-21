package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.aaaaaaah.velcom.shared.protocol.serialization.SerializerBasedTest;
import de.aaaaaaah.velcom.shared.protocol.serialization.Status;
import de.aaaaaaah.velcom.shared.util.LinesWithOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetStatusReplyTest extends SerializerBasedTest {

	@Test
	void deserializeWithNoOptionals() {
		String json = "{\"info\": \"system info goes here\", \"result_available\": false, \"status\": \"IDLE\"}";
		Optional<GetStatusReply> result = serializer.deserialize(json, GetStatusReply.class);

		assertTrue(result.isPresent());
		assertEquals(new GetStatusReply(
				"system info goes here",
				null,
				null,
				false,
				Status.IDLE,
				null,
				null
			),
			result.get()
		);
	}

	@Test
	void deserializeWithAllOptionals() {
		String json = "{"
			+ "\"info\": \"system info goes here\","
			+ " \"version_hash\": \"bloop\","
			+ " \"bench_hash\": \"blabla\","
			+ " \"result_available\": false,"
			+ " \"status\": \"ABORT\","
			+ " \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\","
			+ " \"last_output_lines\": {"
			+ "   \"first_line_offset\": 20,"
			+ "   \"lines\": [\"this is a line\",\"With a newline\"]"
			+ " }"
			+ "}";
		Optional<GetStatusReply> result = serializer.deserialize(json, GetStatusReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertTrue(result.isPresent());
		assertEquals(
			new GetStatusReply(
				"system info goes here",
				"bloop",
				"blabla",
				false,
				Status.ABORT,
				uuid,
				new LinesWithOffset(20, List.of("this is a line", "With a newline"))
			),
			result.get()
		);
	}
}
