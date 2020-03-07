package de.aaaaaaah.velcom.backend.access.entities;

/**
 * A status describing whether a commit needs to be benchmarked or not, and if it needs to be
 * benchmarked, how.
 */
public enum BenchmarkStatus {
	/**
	 * The commit does not need to be benchmarked.
	 */
	NO_BENCHMARK_REQUIRED(0),
	/**
	 * The commit still needs to be benchmarked, or it is currently being benchmarked.
	 */
	BENCHMARK_REQUIRED(1),
	/**
	 * Like BENCHMARK_REQUIRED, but the commit was manually prioritized (i. e. manually (re-)added
	 * to the queue).
	 */
	BENCHMARK_REQUIRED_MANUAL_PRIORITY(2);

	private final int numericalValue;

	BenchmarkStatus(int numericalValue) {
		this.numericalValue = numericalValue;
	}

	/**
	 * Convert an integer into a {@link BenchmarkStatus}. Throws an {@link IllegalArgumentException}
	 * if the integer doesn't correspond to any status.
	 *
	 * @param numericalValue the number to convert into the status
	 * @return the status
	 */
	public static BenchmarkStatus fromNumericalValue(int numericalValue) {
		switch (numericalValue) {
			case 0:
				return NO_BENCHMARK_REQUIRED;
			case 1:
				return BENCHMARK_REQUIRED;
			case 2:
				return BENCHMARK_REQUIRED_MANUAL_PRIORITY;
			default:
				throw new IllegalArgumentException(
					numericalValue + " does not correspond to any benchmark status");
		}
	}

	public int getNumericalValue() {
		return numericalValue;
	}
}
