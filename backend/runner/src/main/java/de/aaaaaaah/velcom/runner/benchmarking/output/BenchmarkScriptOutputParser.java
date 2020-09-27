package de.aaaaaaah.velcom.runner.benchmarking.output;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Benchmark;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Interpretation;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Metric;
import de.aaaaaaah.velcom.shared.util.Either;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The pojo for the output of the benchmark script.
 */
public class BenchmarkScriptOutputParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkScriptOutputParser.class);

	private final ObjectMapper objectMapper = new ObjectMapper()
		.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
		.registerModule(new ParameterNamesModule());

	/**
	 * Parses the benchmark output node to a {@link Result} object.
	 *
	 * @param data the textual data
	 * @return the parsed benchmark results
	 * @throws OutputParseException if an error occurs
	 */
	public Either<String, List<Benchmark>> parse(String data) throws OutputParseException {
		LOGGER.debug("Parsing message '{}'", data);

		JsonNode root;
		try {
			root = objectMapper.readTree(data);
		} catch (JsonProcessingException e) {
			throw new OutputParseException(e.getMessage(), e);
		}

		if (!root.isObject()) {
			throw new OutputParseException("Root is no object");
		}
		// the is object check is needed to allow benchmarks named "error"
		if (root.hasNonNull("error") && !root.get("error").isObject()) {
			if (!root.get("error").isTextual()) {
				throw new OutputParseException("Error is no string: " + root);
			}
			return Either.ofLeft(root.get("error").asText());
		}

		List<Benchmark> benchmarks = new ArrayList<>();

		Iterator<Entry<String, JsonNode>> fields = root.fields();
		while (fields.hasNext()) {
			Entry<String, JsonNode> field = fields.next();
			benchmarks.add(parseBenchmark(field.getKey(), field.getValue()));
		}

		if (benchmarks.isEmpty()) {
			throw new OutputParseException("Root element has no benchmarks");
		}

		return Either.ofRight(benchmarks);
	}

	private Benchmark parseBenchmark(String name, JsonNode node) {
		if (!node.isObject()) {
			throw new OutputParseException("benchmark is no object: " + node);
		}

		List<Metric> metrics = new ArrayList<>();

		Iterator<Entry<String, JsonNode>> fields = node.fields();
		while (fields.hasNext()) {
			Entry<String, JsonNode> field = fields.next();
			metrics.add(parseMetric(field.getKey(), field.getValue()));
		}

		if (metrics.isEmpty()) {
			throw new OutputParseException("Benchmark '" + name + "' has no metric: " + node);
		}

		return new Benchmark(name, metrics);
	}

	private Metric parseMetric(String name, JsonNode node) {
		if (!node.isObject()) {
			throw new OutputParseException("Metric is no object: " + node);
		}

		Optional<String> unit = Optional.empty();
		if (node.hasNonNull("unit")) {
			// parseUnit will throw if the value is invalid
			unit = Optional.of(parseUnit(node.get("unit")));
		}

		Optional<Interpretation> interpretation = getInterpretationFieldName(node)
			.map(node::get)
			// parseInterpretation will throw if the value is invalid
			.map(this::parseInterpretation);

		if (node.hasNonNull("error")) {
			if (!node.get("error").isTextual()) {
				throw new OutputParseException("Error is no string: " + node);
			}

			return new Metric(
				name, node.get("error").asText(), unit.orElse(null), interpretation.orElse(null), null
			);
		} else {
			String resultsFieldName = getResultsFieldName(node)
				.orElseThrow(() -> new OutputParseException("Metric has no results: " + node));

			return new Metric(
				name, null, unit.orElse(null), interpretation.orElse(null),
				parseResults(node.get(resultsFieldName))
			);
		}
	}

	private Optional<String> getInterpretationFieldName(JsonNode node) {
		String fieldName = node.hasNonNull("interpretation")
			? "interpretation"
			: "resultInterpretation";
		if (node.hasNonNull(fieldName)) {
			return Optional.of(fieldName);
		}
		return Optional.empty();
	}

	private Optional<String> getResultsFieldName(JsonNode node) {
		String fieldName = node.hasNonNull("results")
			? "results"
			: "values";
		if (node.hasNonNull(fieldName)) {
			return Optional.of(fieldName);
		}
		return Optional.empty();
	}

	private List<Double> parseResults(JsonNode node) {
		if (!node.isArray()) {
			throw new OutputParseException("Output is no array: " + node);
		}
		int size = node.size();

		List<Double> results = new ArrayList<>(size);

		ArrayNode arrayNode = (ArrayNode) node;

		for (int i = 0; i < size; i++) {
			JsonNode element = arrayNode.get(i);
			if (!element.isNumber()) {
				throw new OutputParseException(
					"Expected a number in: " + node + " at position " + i
				);
			}
			results.add(element.asDouble());
		}

		if (results.isEmpty()) {
			throw new OutputParseException("Expected result to have at least one value!");
		}

		return results;
	}

	private String parseUnit(JsonNode node) {
		if (!node.isTextual()) {
			throw new OutputParseException("Unit is no string: " + node);
		}
		return node.asText();
	}

	private Interpretation parseInterpretation(JsonNode node) {
		if (!node.isTextual()) {
			throw new OutputParseException("Interpretation is no string: " + node);
		}

		try {
			return Interpretation.valueOf(node.asText());
		} catch (IllegalArgumentException e) {
			throw new OutputParseException("Unknown interpretation: " + node);
		}
	}
}
