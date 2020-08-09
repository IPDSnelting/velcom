package de.aaaaaaah.velcom.shared.protocol;

/**
 * The possible reasons when the runner's connection attempt was denied.
 */
public enum RunnerDenyReason {
	TOKEN_INVALID(401, "your token is invalid", "TOKEN"),
	NAME_ALREADY_USED(403, "your name is already taken", "NAME");

	private final int code;
	private final String message;
	private final String headerValue;

	RunnerDenyReason(int code, String message, String headerValue) {
		this.code = code;
		this.message = message;
		this.headerValue = headerValue;
	}

	/**
	 * @return the value the "Runner-Deny" header will contain
	 */
	public String getHeaderValue() {
		return headerValue;
	}

	/**
	 * @return the http response code associated with this event
	 */
	public int getCode() {
		return code;
	}

	/**
	 * @return the reason the runner's connection attempt was denied as a human readable message
	 */
	public String getMessage() {
		return message;
	}
}
