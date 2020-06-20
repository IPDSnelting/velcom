package de.aaaaaaah.velcom.runner.shared.protocol.serialization.serverbound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.EntityTest;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Result.Benchmark;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Result.Interpretation;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Result.Metric;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetResultReplyTest extends EntityTest {

	@Test
	void deserializeRunnerError() throws JsonProcessingException {
		String string = "{\"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\", \"success\": false, \"error\": \"me not worky\"}";
		GetResultReply result = objectMapper.readValue(string, GetResultReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertEquals(
			new GetResultReply(uuid, false, null, "me not worky"),
			result
		);
	}

	@Test
	void deserializeRunnerResultWithoutMetrics() throws JsonProcessingException {
		String string = "{\"run_id\": \"576afdcb-eaf9-46b2-9287-fc3bf8df83df\", \"success\": true, \"result\": {\"error\": \"me also not worky\"}}";
		GetResultReply result = objectMapper.readValue(string, GetResultReply.class);

		UUID uuid = UUID.fromString("576afdcb-eaf9-46b2-9287-fc3bf8df83df");
		assertEquals(
			new GetResultReply(
				uuid,
				true,
				new Result(null, "me also not worky"),
				null
			),
			result
		);
	}

	@Test
	void deserializeRunnerResultWithMetrics() throws JsonProcessingException {
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
			+ "  }"
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
									null, null, null
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
				null
			),
			result
		);
	}
}
