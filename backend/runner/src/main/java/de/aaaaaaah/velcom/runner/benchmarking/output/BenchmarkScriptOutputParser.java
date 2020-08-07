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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
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
	public BareResult parse(String data) throws OutputParseException {
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
			return new BareResult(null, root.get("error").asText());
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

		return new BareResult(benchmarks, null);
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

		if (node.hasNonNull("error")) {
			if (!node.get("error").isTextual()) {
				throw new OutputParseException("Error is no string: " + node);
			}
			Interpretation interpretation = getInterpretationFieldName(node)
				.map(node::get)
				// Will throw if the value is invalid
				.map(this::parseInterpretation)
				.orElse(Interpretation.NEUTRAL);

			String unit = "";
			if (node.hasNonNull("unit")) {
				JsonNode unitNode = node.get("unit");
				if (!unitNode.isTextual()) {
					throw new OutputParseException(
						"Unit '" + unitNode + "' is no string (has type " + unitNode.getNodeType() + ")!"
					);
				}
				unit = unitNode.asText();
			}

			return new Metric(
				name, node.get("error").asText(), unit, interpretation, null
			);
		}

		if (!node.hasNonNull("unit")) {
			throw new OutputParseException("Metric has no unit: " + node);
		}
		if (!node.get("unit").isTextual()) {
			throw new OutputParseException("Unit is no string: " + node);
		}

		String interpretationFieldName = getInterpretationFieldName(node)
			.orElseThrow(() -> new OutputParseException("Metric has no interpretation: " + node));
		String resultsFieldName = getResultsFieldName(node)
			.orElseThrow(() -> new OutputParseException("Metric has no results: " + node));

		String unit = node.get("unit").asText();
		Interpretation interpretation = parseInterpretation(
			node.get(interpretationFieldName)
		);

		return new Metric(
			name, null, unit, interpretation, parseResults(node.get(resultsFieldName))
		);
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

	private Interpretation parseInterpretation(JsonNode node) {
		Interpretation interpretation;
		try {
			interpretation = Interpretation.valueOf(node.asText());
		} catch (IllegalArgumentException e) {
			throw new OutputParseException(
				"Unknown result interpretation '" + node.asText() + "'"
			);
		}
		return interpretation;
	}

	/**
	 * A bare result with just the parsed properties and errors.
	 */
	public static class BareResult {

		@Nullable
		private final List<Benchmark> benchmarks;
		@Nullable
		private final String error;

		public BareResult(@Nullable List<Benchmark> benchmarks, @Nullable String error) {
			// TODO maybe use Either?
			if (benchmarks == null && error == null) {
				throw new IllegalArgumentException("benchmarks and error must not both be null");
			} else if (benchmarks != null && error != null) {
				throw new IllegalArgumentException(
					"benchmarks and error can not both be set at the same time");
			}

			this.benchmarks = benchmarks;
			this.error = error;
		}

		@Nullable
		public List<Benchmark> getBenchmarks() {
			return benchmarks;
		}

		@Nullable
		public String getError() {
			return error;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			BareResult that = (BareResult) o;
			return Objects.equals(benchmarks, that.benchmarks) &&
				Objects.equals(error, that.error);
		}

		@Override
		public int hashCode() {
			return Objects.hash(benchmarks, error);
		}
	}
}
