package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Objects;

/**
 * A "dumb" class that identifies a {@link Measurement} in a {@link Run}.
 */
public class Dimension {

	private final String benchmark;
	private final String metric;

	public Dimension(String benchmark, String metric) {
		this.benchmark = Objects.requireNonNull(benchmark);
		this.metric = Objects.requireNonNull(metric);
	}

	public String getBenchmark() {
		return benchmark;
	}

	public String getMetric() {
		return metric;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Dimension that = (Dimension) o;
		return Objects.equals(benchmark, that.benchmark) &&
			Objects.equals(metric, that.metric);
	}

	@Override
	public int hashCode() {
		return Objects.hash(benchmark, metric);
	}

	@Override
	public String toString() {
		return "MeasurementName{" +
			"benchmark='" + benchmark + '\'' +
			", metric='" + metric + '\'' +
			'}';
	}
}
