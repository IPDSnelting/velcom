package de.aaaaaaah.designproto.backend.data.linearlog;

// TODO make unchecked?

/**
 * This exception is thrown when something goes wrong while a log is being linearized.
 */
public class LinearLogException extends Exception {

	public LinearLogException() {
	}

	public LinearLogException(String message) {
		super(message);
	}

	public LinearLogException(String message, Throwable cause) {
		super(message, cause);
	}

	public LinearLogException(Throwable cause) {
		super(cause);
	}

	public LinearLogException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
