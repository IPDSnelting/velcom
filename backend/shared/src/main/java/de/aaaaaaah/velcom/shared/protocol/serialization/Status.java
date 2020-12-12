package de.aaaaaaah.velcom.shared.protocol.serialization;

/**
 * This enum describes a runner's state from the view of a backend it is connected to.
 */
public enum Status {
	/**
	 * The runner is running a benchmark.
	 */
	RUN,

	/**
	 * The runner is aborting a benchmark.
	 */
	ABORT,

	/**
	 * The runner is not running a benchmark.
	 */
	IDLE
}
