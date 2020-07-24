package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Benchmark;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Interpretation;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Metric;
import de.aaaaaaah.velcom.shared.protocol.serialization.SerializerBasedTest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetResultReplyTest extends SerializerBasedTest {

	@Test
	void deserializeRunnerError() {
		Instant targetInstant = Instant.parse("2020-02-10T00:00:00Z");
		String string = "{"
			+ "  \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\","
			+ "  \"success\": false,"
			+ "  \"error\": \"me not worky\","
			+ "  \"start_time\": \"2020-02-10T00:00:00Z\","
			+ "  \"stop_time\": \"2020-02-10T00:00:00Z\""
			+ "}";
		Optional<GetResultReply> result = serializer.deserialize(string, GetResultReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertTrue(result.isPresent());
		assertEquals(
			new GetResultReply(uuid, false, null, "me not worky", targetInstant, targetInstant),
			result.get()
		);
	}

	@Test
	void deserializeRunnerResultWithoutMetrics() {
		Instant targetInstant = Instant.parse("2020-02-10T00:00:00Z");
		String string = "{"
			+ "  \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\","
			+ "  \"success\": true,"
			+ "  \"result\": {\"error\": \"me also not worky\"},"
			+ "  \"start_time\": \"2020-02-10T00:00:00Z\","
			+ "  \"stop_time\": \"2020-02-10T00:00:00Z\""
			+ "}";
		Optional<GetResultReply> result = serializer.deserialize(string, GetResultReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertTrue(result.isPresent());
		assertEquals(
			new GetResultReply(
				uuid,
				true,
				new Result(null, "me also not worky"),
				null,
				targetInstant,
				targetInstant
			),
			result.get()
		);
	}

	@Test
	void deserializeRunnerResultWithoutStopTime() {
		String string = "{"
			+ "  \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\","
			+ "  \"success\": true,"
			+ "  \"result\": {\"error\": \"me also not worky\"},"
			+ "  \"start_time\": \"2020-02-10T00:00:00Z\""
			+ "}";
		Optional<GetResultReply> result = serializer.deserialize(string, GetResultReply.class);
		assertTrue(result.isEmpty());
	}

	@Test
	void deserializeRunnerResultWithoutStartTime() {
		String string = "{"
			+ "  \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\","
			+ "  \"success\": true,"
			+ "  \"result\": {\"error\": \"me also not worky\"},"
			+ "  \"stop_time\": \"2020-02-10T00:00:00Z\""
			+ "}";
		Optional<GetResultReply> result = serializer.deserialize(string, GetResultReply.class);
		assertTrue(result.isEmpty());
	}

	@Test
	void deserializeRunnerResultWithMetrics() {
		Instant start = Instant.parse("2020-02-10T00:00:00Z");
		Instant stop = Instant.parse("2020-05-10T00:00:20Z");
		String string = "{"
			+ "  \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\","
			+ "  \"success\": true,"
			+ "  \"result\": {"
			+ "    \"benchmarks\": ["
			+ "      {"
			+ "        \"name\": \"sample benchmark\","
			+ "        \"metrics\": ["
			+ "          {"
			+ "            \"name\": \"sample metric\","
			+ "            \"unit\": \"barn\","
			+ "            \"interpretation\": \"MORE_IS_BETTER\","
			+ "            \"error\": \"something went wrong\""
			+ "          },"
			+ "          {"
			+ "            \"name\": \"other metric\","
			+ "            \"unit\": \"seconds\","
			+ "            \"interpretation\": \"LESS_IS_BETTER\","
			+ "            \"values\": [1, 2, 3]"
			+ "          }"
			+ "        ]"
			+ "      }"
			+ "    ]"
			+ "  },"
			+ "  \"start_time\": \"2020-02-10T00:00:00Z\","
			+ "  \"stop_time\": \"2020-05-10T00:00:20Z\""
			+ "}";
		Optional<GetResultReply> result = serializer.deserialize(string, GetResultReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertTrue(result.isPresent());
		assertEquals(
			new GetResultReply(
				uuid,
				true,
				new Result(
					List.of(
						new Benchmark(
							"sample benchmark",
							List.of(
								new Metric(
									"sample metric",
									"something went wrong",
									"barn",
									Interpretation.MORE_IS_BETTER,
									null
								),
								new Metric(
									"other metric",
									null,
									"seconds",
									Interpretation.LESS_IS_BETTER,
									List.of(1.0, 2.0, 3.0)
								)
							)
						)
					),
					null
				),
				null,
				start,
				stop
			),
			result.get()
		);
	}
}
