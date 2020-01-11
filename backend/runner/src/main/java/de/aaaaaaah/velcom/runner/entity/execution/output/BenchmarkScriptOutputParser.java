package de.aaaaaaah.velcom.runner.entity.execution.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults.Benchmark;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults.Metric;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults.MetricInterpretation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * The pojo for the output of the benchmark script.
 */
public class BenchmarkScriptOutputParser {

	/**
	 * Parses the benchmark output node to a {@link BenchmarkResults} object.
	 *
	 * @param workOrder the initial work order
	 * @param root the root node of the script output
	 * @return the parsed benchmark results
	 * @throws OutputParseException if an error occurs
	 */
	public BenchmarkResults parse(RunnerWorkOrder workOrder, JsonNode root)
		throws OutputParseException {
		System.err.println("Parsing: ");
		System.err.println(root);
		if (!root.isObject()) {
			throw new OutputParseException("Root is no object");
		}
		if (root.hasNonNull("error")) {
			if (!root.get("error").isTextual()) {
				throw new OutputParseException("Error is no string: " + root);
			}
			return new BenchmarkResults(workOrder, root.get("error").asText());
		}

		List<Benchmark> benchmarks = new ArrayList<>();

		Iterator<Entry<String, JsonNode>> fields = root.fields();
		while (fields.hasNext()) {
			Entry<String, JsonNode> field = fields.next();
			benchmarks.add(parseBenchmark(field.getKey(), field.getValue()));
		}

		return new BenchmarkResults(workOrder, benchmarks);
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
			return new Metric(
				name, "", MetricInterpretation.NEUTRAL, List.of(), node.get("error").asText()
			);
		}

		if (!node.hasNonNull("unit")) {
			throw new OutputParseException("Metric has no unit: " + node);
		}
		if (!node.get("unit").isTextual()) {
			throw new OutputParseException("Unit is no string: " + node);
		}
		if (!node.hasNonNull("result_interpretation")) {
			throw new OutputParseException("Metric has no interpretation: " + node);
		}
		if (!node.hasNonNull("results")) {
			throw new OutputParseException("Metric has no results: " + node);
		}

		String unit = node.get("unit").asText();
		MetricInterpretation interpretation = parseInterpretation(
			node.get("result_interpretation")
		);

		return new Metric(
			name, unit, interpretation, parseResults(node.get("results")), null
		);
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
					"Exepected a number in: " + node + " at position " + i
				);
			}
			results.add(element.asDouble());
		}

		return results;
	}

	private MetricInterpretation parseInterpretation(JsonNode node) {
		MetricInterpretation interpretation;
		try {
			interpretation = MetricInterpretation.valueOf(node.asText());
		} catch (IllegalArgumentException e) {
			throw new OutputParseException(
				"Unknown result interpretation " + node.get("interpretation")
			);
		}
		return interpretation;
	}
}
