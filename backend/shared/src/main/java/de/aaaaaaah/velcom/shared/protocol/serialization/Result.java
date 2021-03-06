package de.aaaaaaah.velcom.shared.protocol.serialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * The result of a run, in a format that is easy to serialize and deserialize.
 */
public class Result {

	@Nullable
	private final List<Benchmark> benchmarks;
	@Nullable
	private final String error;

	public Result(@Nullable List<Benchmark> benchmarks, @Nullable String error) {
		if (benchmarks == null && error == null) {
			throw new IllegalArgumentException("benchmarks and error can't both be null");
		} else if (benchmarks != null && error != null) {
			throw new IllegalArgumentException("benchmarks and error can't both contain a value");
		}

		this.benchmarks = benchmarks;
		this.error = error;
	}

	public Optional<List<Benchmark>> getBenchmarks() {
		return Optional.ofNullable(benchmarks);
	}

	public Optional<String> getError() {
		return Optional.ofNullable(error);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Result result = (Result) o;
		return Objects.equals(benchmarks, result.benchmarks) &&
			Objects.equals(error, result.error);
	}

	@Override
	public int hashCode() {
		return Objects.hash(benchmarks, error);
	}

	/**
	 * Possible result interpretations for a single metric.
	 */
	public enum Interpretation {
		LESS_IS_BETTER,
		MORE_IS_BETTER,
		NEUTRAL
	}

	/**
	 * A benchmark contains multiple metrics.
	 */
	public static class Benchmark {

		private final String name;
		private final List<Metric> metrics;

		@JsonCreator
		public Benchmark(
			@JsonProperty(required = true) String name,
			@JsonProperty(required = true) List<Metric> metrics
		) {
			this.name = name;
			this.metrics = metrics;
		}

		public String getName() {
			return name;
		}

		public List<Metric> getMetrics() {
			return metrics;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Benchmark benchmark = (Benchmark) o;
			return name.equals(benchmark.name) &&
				metrics.equals(benchmark.metrics);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, metrics);
		}

		@Override
		public String toString() {
			return "Benchmark{" +
				"name='" + name + '\'' +
				", metrics=" + metrics +
				'}';
		}
	}

	/**
	 * A metric is either successful or failed, If it is successful, it contains the measured values
	 * and some extra information. If it is failed, it contains only an error message.
	 */
	public static class Metric {

		private final String name;
		@Nullable
		private final String error;
		@Nullable
		private final String unit;
		@Nullable
		private final Interpretation interpretation;
		@Nullable
		private final List<Double> values;

		@JsonCreator
		public Metric(
			@JsonProperty(required = true) String name,
			@Nullable String error,
			@Nullable String unit,
			@Nullable Interpretation interpretation,
			@Nullable List<Double> values
		) {
			if (error == null && values == null) {
				throw new IllegalArgumentException("if error is null, values must not be null");
			}

			this.name = name;
			this.error = error;
			this.unit = unit;
			this.interpretation = interpretation;
			this.values = values;
		}

		public String getName() {
			return name;
		}

		public Optional<String> getError() {
			return Optional.ofNullable(error);
		}

		public Optional<String> getUnit() {
			return Optional.ofNullable(unit);
		}

		public Optional<Interpretation> getInterpretation() {
			return Optional.ofNullable(interpretation);
		}

		public Optional<List<Double>> getValues() {
			return Optional.ofNullable(values);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Metric metric = (Metric) o;
			return Objects.equals(error, metric.error) &&
				Objects.equals(name, metric.name) &&
				Objects.equals(unit, metric.unit) &&
				interpretation == metric.interpretation &&
				Objects.equals(values, metric.values);
		}

		@Override
		public int hashCode() {
			return Objects.hash(error, name, unit, interpretation, values);
		}

		@Override
		public String toString() {
			return "Metric{" +
				"name='" + name + '\'' +
				", error='" + error + '\'' +
				", unit='" + unit + '\'' +
				", interpretation=" + interpretation +
				", values=" + values +
				'}';
		}
	}
}
