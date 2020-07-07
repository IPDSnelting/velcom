package de.aaaaaaah.velcom.shared;

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
	 * Runner is preparing work, updating the repo or receiving a work binary.
	 */
	PREPARING_WORK,
	/**
	 * The runner is currently disconnected.
	 */
	DISCONNECTED
}
