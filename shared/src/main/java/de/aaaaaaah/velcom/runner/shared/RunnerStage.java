package de.aaaaaaah.velcom.runner.shared;

/**
 * The different stages a runner can be in.
 */
public enum RunnerStage {
	/**
	 * The runner stage is not known.
	 */
	UNKNOWN,
	/**
	 * The runner is initializing and identifying itself to the server.
	 */
	INITIALIZATION,
	/**
	 * The runner is idle.
	 */
	IDLE,
	/**
	 * The runner is currently fetching work from the server.
	 */
	FETCHING_WORK,
	/**
	 * The runner is executing the work.
	 */
	EXECUTING,
	/**
	 * The runner is transmitting its results.
	 */
	SENDING_RESULTS,
}
