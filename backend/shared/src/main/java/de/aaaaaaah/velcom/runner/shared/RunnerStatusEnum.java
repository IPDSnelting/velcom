package de.aaaaaaah.velcom.runner.shared;

/**
 * The state the runner can be in.
 */
public enum RunnerStatusEnum {
	/**
	 * The runner is connected and idle.
	 */
	IDLE,
	/**
	 * The runner is connected but not ready to accept jobs.
	 */
	INITIALIZING,
	/**
	 * The runner is currently working.
	 */
	WORKING,
	/**
	 * The runner is currently disconnected.
	 */
	DISCONNECTED
}
