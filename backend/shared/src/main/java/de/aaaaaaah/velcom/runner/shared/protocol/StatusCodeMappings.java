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

	/**
	 * The server tried to tell the client to abort a commit, but it failed.
	 */
	public static final int CLIENT_FAILED_TO_CANCEL = 4004;

	/**
	 * Dispatching the commit failed. The runner should <em>not report its results back</em>!
	 */
	public static final String DISPATCH_FAILED_DISCARD_RESULTS = "Dispatch failed, discard results";

	private StatusCodeMappings() {
		throw new UnsupportedOperationException("No instantiation");
	}

}
