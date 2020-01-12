package de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains the results of a benchmark.
 */
public class BenchmarkResults implements SentEntity {

	private final RunnerWorkOrder workOrder;
	private final List<Benchmark> benchmarks;
	private final String error;
	private final Instant startTime;
	private final Instant endTime;

	/**
	 * Creates a new {@link BenchmarkResults} packet.
	 *
	 * @param workOrder the work order that initiated the benchmark
	 * @param benchmarks the benchmarks
	 * @param error the error message. Null if there was none.
	 * @param startTime the time the benchmark script started executing
	 * @param endTime the time the benchmark script finished executing
	 */
	@JsonCreator
	public BenchmarkResults(RunnerWorkOrder workOrder, List<Benchmark> benchmarks,
		String error, Instant startTime, Instant endTime) {
		this.workOrder = workOrder;
		this.benchmarks = benchmarks == null ? List.of() : new ArrayList<>(benchmarks);
		this.error = error;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	/**
	 * Creates a new {@link BenchmarkResults} packet.
	 *
	 * @param workOrder the work order that initiated the benchmark
	 * @param benchmarks the benchmarks
	 * @param startTime the time the benchmark script started executing
	 * @param endTime the time the benchmark script finished executing
	 */
	public BenchmarkResults(RunnerWorkOrder workOrder, List<Benchmark> benchmarks,
		Instant startTime, Instant endTime) {
		this.workOrder = workOrder;
		this.benchmarks = benchmarks == null ? List.of() : new ArrayList<>(benchmarks);
		this.startTime = startTime;
		this.endTime = endTime;
		this.error = null;
	}

	/**
	 * Creates a new {@link BenchmarkResults} packet.
	 *
	 * @param workOrder the work order that initiated the benchmark
	 * @param error the error message. Null if there was none.
	 * @param startTime the time the benchmark script started executing
	 * @param endTime the time the benchmark script finished executing
	 */
	public BenchmarkResults(RunnerWorkOrder workOrder, String error, Instant startTime,
		Instant endTime) {
		this.workOrder = workOrder;
		this.startTime = startTime;
		this.endTime = endTime;
		this.benchmarks = Collections.emptyList();
		this.error = error;
	}

	/**
	 * Returns all benchmarks. Might be empty.
	 *
	 * @return all benchmarks
	 */
	public List<Benchmark> getBenchmarks() {
		return Collections.unmodifiableList(benchmarks);
	}

	/**
	 * Returns the error message, if any. Null otherwise.
	 *
	 * @return the error message, if any
	 */
	@JsonProperty
	public String getError() {
		return error;
	}

	/**
	 * Returns whether this benchmark has an error. If this is true, {@link #getError()} will return
	 * a message
	 *
	 * @return true if this benchmark has an error
	 */
	@JsonIgnore
	public boolean isError() {
		return error != null;
	}

	/**
	 * @return the time the benchmark script started executing
	 */
	public Instant getStartTime() {
		return startTime;
	}

	/**
	 * @return the time the benchmark script finished executing
	 */
	public Instant getEndTime() {
		return endTime;
	}

	/**
	 * Returns the work order that initiated the benchmark.
	 *
	 * @return the work order that initiated the benchmark
	 */
	public RunnerWorkOrder getWorkOrder() {
		return workOrder;
	}

	@Override
	public String toString() {
		return "BenchmarkResults{" +
			"workOrder=" + workOrder +
			", benchmarks=" + benchmarks +
			", error='" + error + '\'' +
			", startTime=" + startTime +
			", endTime=" + endTime +
			'}';
	}

	/**
	 * The interpretation of the metric.
	 */
	public enum MetricInterpretation {
		LESS_IS_BETTER,
		MORE_IS_BETTER,
		NEUTRAL
	}

	/**
	 * A single benchmark, consisting of multiple {@link Metric}s.
	 */
	public static class Benchmark {

		private String name;
		private List<Metric> metrics;

		/**
		 * Creates a new benchmark.
		 *
		 * @param name the name of the benchmark
		 * @param metrics the contained metrics
		 */
		public Benchmark(String name, List<Metric> metrics) {
			this.name = name;
			this.metrics = new ArrayList<>(metrics);
		}

		/**
		 * Returns the name of the benchmark.
		 *
		 * @return the name of the benchmark
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns all contained {@link Metric}s.
		 *
		 * @return all contained metrics
		 */
		public List<Metric> getMetrics() {
			return metrics;
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
	 * A single metric with a name, units, an interpretation and values.
	 */
	public static class Metric {

		private String error;
		private String name;
		private String unit;
		private MetricInterpretation resultInterpretation;
		private List<Double> results;

		/**
		 * Creates a new metric.
		 *
		 * @param name the name of the metric
		 * @param unit the unit of the metric
		 * @param resultInterpretation the interpretation of the metric
		 * @param results the values
		 * @param error the error message
		 */
		@JsonCreator
		public Metric(String name, String unit,
			MetricInterpretation resultInterpretation, List<Double> results, String error) {
			this.name = name;
			this.unit = unit;
			this.resultInterpretation = resultInterpretation;
			this.results = results == null ? List.of() : new ArrayList<>(results);
			this.error = error;
		}

		/**
		 * Returns the name of the metric.
		 *
		 * @return the name of the metric
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the unit of the metric.
		 *
		 * @return the unit of the metric
		 */
		public String getUnit() {
			return unit;
		}

		/**
		 * Returns the interpretation of the metric.
		 *
		 * @return the interpretation of the metric
		 */
		public MetricInterpretation getResultInterpretation() {
			return resultInterpretation;
		}

		/**
		 * Returns the error message, if any. Null otherwise.
		 *
		 * @return the error message, if any
		 */
		@JsonProperty
		public String getError() {
			return error;
		}

		/**
		 * Returns whether this benchmark has an error. If this is true, {@link #getError()} will
		 * return a message
		 *
		 * @return true if this benchmark has an error
		 */
		@JsonIgnore
		public boolean isError() {
			return error != null;
		}

		/**
		 * Returns the values.
		 *
		 * @return the values
		 */
		public List<Double> getResults() {
			return Collections.unmodifiableList(results);
		}

		@Override
		public String toString() {
			return "Metric{" +
				"error='" + error + '\'' +
				", name='" + name + '\'' +
				", unit='" + unit + '\'' +
				", interpretation=" + resultInterpretation +
				", results=" + results +
				'}';
		}
	}
}
