package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.shared.protocol.serialization.EntityTest;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Benchmark;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Interpretation;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Metric;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetResultReplyTest extends EntityTest {

	@Test
	void deserializeRunnerError() throws JsonProcessingException {
		Instant targetInstant = Instant.parse("2020-02-10T00:00:00Z");
		String string = "{"
			+ "  \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\","
			+ "  \"success\": false,"
			+ "  \"error\": \"me not worky\","
			+ "  \"start_time\": \"2020-02-10T00:00:00Z\","
			+ "  \"stop_time\": \"2020-02-10T00:00:00Z\""
			+ "}";
		GetResultReply result = objectMapper.readValue(string, GetResultReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertEquals(
			new GetResultReply(uuid, false, null, "me not worky", targetInstant, targetInstant),
			result
		);
	}

	@Test
	void deserializeRunnerResultWithoutMetrics() throws JsonProcessingException {
		Instant targetInstant = Instant.parse("2020-02-10T00:00:00Z");
		String string = "{"
			+ "  \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\","
			+ "  \"success\": true,"
			+ "  \"result\": {\"error\": \"me also not worky\"},"
			+ "  \"start_time\": \"2020-02-10T00:00:00Z\","
			+ "  \"stop_time\": \"2020-02-10T00:00:00Z\""
			+ "}";
		GetResultReply result = objectMapper.readValue(string, GetResultReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertEquals(
			new GetResultReply(
				uuid,
				true,
				new Result(null, "me also not worky"),
				null,
				targetInstant,
				targetInstant
			),
			result
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

		assertThatThrownBy(() -> objectMapper.readValue(string, GetResultReply.class))
			.isInstanceOf(JsonProcessingException.class)
			.hasMessageContaining("stop_time");
	}

	@Test
	void deserializeRunnerResultWithoutStartTime() {
		String string = "{"
			+ "  \"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\","
			+ "  \"success\": true,"
			+ "  \"result\": {\"error\": \"me also not worky\"},"
			+ "  \"stop_time\": \"2020-02-10T00:00:00Z\""
			+ "}";

		assertThatThrownBy(() -> objectMapper.readValue(string, GetResultReply.class))
			.isInstanceOf(JsonProcessingException.class)
			.hasMessageContaining("start_time");
	}

	@Test
	void deserializeRunnerResultWithMetrics() throws JsonProcessingException {
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
		GetResultReply result = objectMapper.readValue(string, GetResultReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
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
			result
		);
	}
}
