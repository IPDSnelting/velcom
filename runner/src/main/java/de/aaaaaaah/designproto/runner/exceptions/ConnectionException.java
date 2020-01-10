package de.aaaaaaah.designproto.runner.exceptions;

/**
 * An exception indicating that the connection with the server could not be established.
 */
public class ConnectionException extends RuntimeException {

	/**
	 * Creates a new connection exception.
	 *
	 * @param message the detail message
	 */
	public ConnectionException(String message) {
		super(message);
	}

	/**
	 * Creates a new connection exception.
	 *
	 * @param message the detail message
	 * @param cause the underlying cause
	 */
	public ConnectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
