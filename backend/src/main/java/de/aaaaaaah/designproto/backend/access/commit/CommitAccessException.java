package de.aaaaaaah.designproto.backend.access.commit;

/**
 * This exception is thrown when something goes wrong while accessing commits in a repository.
 */
public class CommitAccessException extends Exception {

	public CommitAccessException() {
	}

	public CommitAccessException(String message) {
		super(message);
	}

	public CommitAccessException(String message, Throwable cause) {
		super(message, cause);
	}

	public CommitAccessException(Throwable cause) {
		super(cause);
	}

	public CommitAccessException(String message, Throwable cause, boolean enableSuppression,
		boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
