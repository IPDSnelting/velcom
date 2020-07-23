package de.aaaaaaah.velcom.runner.revision;

import java.time.Duration;

/**
 * A class for keeping all kinds of runner-related delays and timeouts.
 */
public class Delays {

	/**
	 * How long to wait for a reconnect when creating a connection fails. This delay prevents
	 * flooding the server on the other end with too many connection attempts.
	 */
	public static final Duration RECONNECT_AFTER_FAILED_CONNECTION = Duration.ofSeconds(10);

	/**
	 * How long to wait for a server response when trying to close a connection before force-closing
	 * it.
	 */
	public static final Duration CLOSE_CONNECTION_TIMEOUT = Duration.ofSeconds(10);

	/**
	 * How long to pause in-between rounds of asking the backends for new benchmarks.
	 */
	public static final Duration BACKEND_ROUNDTRIP = Duration.ofSeconds(10);

	/**
	 * How long to wait - after sending a command to the server - for a reply to that command.
	 */
	public static final Duration AWAIT_COMMAND_REPLY = Duration.ofSeconds(10);

	/**
	 * How long to wait for the next partial packet in a stream of binary packets.
	 */
	public static final Duration NEXT_PARTIAL_BINARY_PACKET = Duration.ofSeconds(1);

	/**
	 * How long to wait between multiple packets that should be sent one after the other.
	 */
	public static final Duration FOLLOW_UP_PACKET = Duration.ofSeconds(1);

	private Delays() {
		throw new UnsupportedOperationException();
	}
}
