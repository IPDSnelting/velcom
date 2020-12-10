package de.aaaaaaah.velcom.backend.runner;

import java.time.Duration;

/**
 * A class for keeping all kinds of runner-related delays and timeouts.
 */
public final class Delays {

	/**
	 * How long to wait for a runner response when trying to close a connection before force-closing
	 * it.
	 */
	public static final Duration CLOSE_CONNECTION_TIMEOUT = Duration.ofSeconds(10);

	/**
	 * How long to wait - after sending a command to the server - for a reply to that command.
	 */
	public static final Duration AWAIT_COMMAND_REPLY = Duration.ofSeconds(10);

	/**
	 * How long to wait between "get_status" commands. This affects the update interval of the live
	 * build log.
	 */
	public static final Duration REQUEST_STATUS_INTERVAL = Duration.ofSeconds(5);

	private Delays() {
		throw new UnsupportedOperationException();
	}
}
