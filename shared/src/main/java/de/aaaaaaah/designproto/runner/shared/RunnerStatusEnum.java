package de.aaaaaaah.designproto.runner.shared;

/**
 * The state the runner can be in.
 */
public enum RunnerStatusEnum {
	/**
	 * The runner is connected and idle.
	 */
	IDLE,
	/**
	 * The runner is currently working.
	 */
	WORKING,
	/**
	 * The runner is currently disconnected.
	 */
	DISCONNECTED
}
