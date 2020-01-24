package de.aaaaaaah.velcom.runner.shared.protocol;

/**
 * Contains mapping for status codes used in the runner protocol.
 */
public class StatusCodeMappings {

	/**
	 * The client disconnected willingly.
	 */
	public static int CLIENT_ORDERLY_DISCONNECT = 4000;

	/**
	 * Server initiated close.
	 */
	public static int SERVER_INITIATED_DISCONNECT = 4002;

	/**
	 * Server initiated close as a runner with the same name connected.
	 */
	public static int NAME_ALREADY_TAKEN = 4003;
}
