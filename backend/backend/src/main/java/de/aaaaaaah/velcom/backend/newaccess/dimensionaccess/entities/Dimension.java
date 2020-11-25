package de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities;

import java.util.Comparator;
import java.util.Objects;

/**
 * A dimension consists of a benchmark and a metric and can uniquely identify a measurement in a
 * run.
 */
public class Dimension implements Comparable<Dimension> {

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
		return "Dimension{" +
			"benchmark='" + benchmark + '\'' +
			", metric='" + metric + '\'' +
			'}';
	}

	@Override
	public int compareTo(Dimension other) {
		return Comparator.comparing(Dimension::getBenchmark)
			.thenComparing(Dimension::getMetric)
			.compare(this, other);
	}
}
