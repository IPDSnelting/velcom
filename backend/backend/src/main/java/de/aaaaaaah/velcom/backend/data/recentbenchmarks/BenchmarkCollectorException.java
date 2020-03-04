package de.aaaaaaah.velcom.backend.data.recentbenchmarks;

/**
 * An exception that occurs when the {@link RecentBenchmarkCollector} is run and encountered some
 * exception.
 */
public class BenchmarkCollectorException extends RuntimeException {

	BenchmarkCollectorException(Throwable cause) {
		super(cause);
	}

}
