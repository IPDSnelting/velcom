package de.aaaaaaah.velcom.runner.benchmarking.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Benchmark;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Interpretation;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result.Metric;
import de.aaaaaaah.velcom.shared.util.Either;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;

class BenchmarkScriptOutputParserTest {

	private BenchmarkScriptOutputParser parser;

	@BeforeEach
	void setUp() {
		parser = new BenchmarkScriptOutputParser();
	}

	@ParameterizedTest
	@CsvFileSource(resources = {"/benchmark-script/valid-input.csv"}, delimiter = '|')
	void parseSomeValidInput(boolean error, String line) {
		Either<String, List<Benchmark>> result = parser.parse(line);
		if (error) {
			assertThat(result.isLeft()).isTrue();
		} else {
			assertThat(result.isRight()).isTrue();
		}
	}

	@Test
	void parseInvalidJson() {
		assertThatThrownBy(() -> parser.parse("{ hey"))
			.isInstanceOf(OutputParseException.class);
	}

	@ParameterizedTest
	@CsvSource(value = {
		"{\"benchmark\": [] }",
		"{\"benchmark\": 20 }",
		"{\"benchmark\": false }",
		"{\"benchmark\": \"hey\" }",
		"{\"benchmark\": null }",
	}, delimiter = '|')
	void parseSemanticallyInvalidJson(String line) {
		assertThatThrownBy(() -> parser.parse(line))
			.isInstanceOf(OutputParseException.class);
	}

	@Test
	void parseMetricWithErrorAndOtherStuff() {
		Either<String, List<Benchmark>> result = parser.parse(
			"{ \"benchmarks\": { \"test\": { \"unit\": \"cats\", \"error\": \"Hey\" } } }"
		);

		assertThat(result.getRight()).isPresent();
		Benchmark benchmark = result.getRight().get().get(0);
		assertThat(benchmark.getName()).isEqualTo("benchmarks");

		assertThat(benchmark.getMetrics()).isNotEmpty();
		Metric metric = benchmark.getMetrics().get(0);
		assertThat(metric.getName()).isEqualTo("test");
		assertThat(metric.getError()).isPresent();
		assertThat(metric.getError().get()).isEqualTo("Hey");
	}

	@Test
	void parseMetricWithInvalidInterpretation() {
		String data = "{ \"test\": { \"metric\": { \"results\": [ 1341173530.8587837 ],"
			+ " \"unit\": \"hope's\", \"resultInterpretation\": \"NOT VALID\" } } }";
		assertThatThrownBy(() -> parser.parse(data)).isInstanceOf(OutputParseException.class);
	}

	@ParameterizedTest
	@CsvSource(value = {
		"{ \"test\": { \"metric\": { \"error\": 20 } } }",
		"{ \"test\": { \"metric\": { \"error\": false } } }",
		"{ \"test\": { \"metric\": { \"error\": {} } } }",
		"{ \"test\": { \"metric\": { \"error\": [] } } }",
		"{ \"error\": 20 }",
		"{ \"error\": false }",
		"{ \"error\": [] }",
	}, delimiter = '|')
	void parseInvalidErrorMessage(String data) {
		assertThatThrownBy(() -> parser.parse(data)).isInstanceOf(OutputParseException.class);
	}

	@ParameterizedTest
	@CsvFileSource(resources = {"/benchmark-script/invalid-metric-input.csv"}, delimiter = '|')
	void parseInvalidMetric(String data) {
		assertThatThrownBy(() -> parser.parse(data)).isInstanceOf(OutputParseException.class);
	}

	@ParameterizedTest
	@CsvSource(value = {
		"{}",
		"[]",
		"\"hey\"",
		"20",
		"false",
		"null",
	}, delimiter = '|')
	void parseInvalidRootObjects(String data) {
		assertThatThrownBy(() -> parser.parse(data)).isInstanceOf(OutputParseException.class);
	}

	@Test
	void parseMetricCalledError() {
		Either<String, List<Benchmark>> result = parser.parse(
			"{ \"error\": { \"test\": { \"unit\": \"cats\", \"error\": \"Hey\" } } }"
		);

		assertThat(result.getRight()).isPresent();
		Benchmark benchmark = result.getRight().get().get(0);
		assertThat(benchmark.getName()).isEqualTo("error");

		assertThat(benchmark.getMetrics()).isNotEmpty();
		Metric metric = benchmark.getMetrics().get(0);
		assertThat(metric.getName()).isEqualTo("test");
		assertThat(metric.getError()).isPresent();
		assertThat(metric.getError().get()).isEqualTo("Hey");
	}

	@Test
	void parseInvalidInterpretationInErrorMessage() {
		assertThatThrownBy(() -> parser.parse(
			"{ \"test\": { \"metric\": { \"error\": \"Yes\", \"interpretation\": \"WHO KNOWS\" } } }"
		))
			.isInstanceOf(OutputParseException.class)
			.hasMessageContaining("WHO KNOWS");
	}

	@Test
	void parseInvalidUnitInErrorMessage() {
		assertThatThrownBy(() -> parser.parse(
			"{ \"test\": { \"metric\": { \"error\": \"Yes\", \"unit\": 42 } } }"
		))
			.isInstanceOf(OutputParseException.class)
			.hasMessageContaining("42");
	}

	@Test
	void parsedMixOldNew() {
		// Old values, new interpretation
		assertThat(parser.parse(
			"{ \"test\": { \"metric\": { \"values\": [1,2], \"unit\": \"unit\","
				+ " \"interpretation\": \"NEUTRAL\" } } }"
		))
			.isEqualTo(Either.ofRight(List.of(new Benchmark(
				"test",
				List.of(new Metric("metric", null, "unit", Interpretation.NEUTRAL, List.of(1.0, 2.0)))
			))));

		// new results, old resultInterpretation
		assertThat(parser.parse(
			"{ \"test\": { \"metric\": { \"results\": [1,2], \"unit\": \"unit\","
				+ " \"resultInterpretation\": \"NEUTRAL\" } } }"
		))
			.isEqualTo(Either.ofRight(List.of(new Benchmark(
				"test",
				List.of(new Metric("metric", null, "unit", Interpretation.NEUTRAL, List.of(1.0, 2.0)))
			))));
	}
}
