package de.aaaaaaah.velcom.runner.shared.protocol;

/**
 * Contains mapping for status codes used in the runner protocol.
 */
public final class StatusCodeMappings {

	/**
	 * The client disconnected willingly.
	 */
	public static final int CLIENT_ORDERLY_DISCONNECT = 4000;

	/**
	 * Server initiated close.
	 */
	public static final int SERVER_INITIATED_DISCONNECT = 4002;

	/**
	 * Server initiated close as a runner with the same name connected.
	 */
	public static final int NAME_ALREADY_TAKEN = 4003;

	private StatusCodeMappings() {
		throw new UnsupportedOperationException("No instantiation");
	}

}
