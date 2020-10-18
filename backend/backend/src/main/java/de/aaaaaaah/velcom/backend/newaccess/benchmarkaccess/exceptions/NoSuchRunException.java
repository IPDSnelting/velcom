package de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.exceptions;

import de.aaaaaaah.velcom.backend.access.entities.RunId;

/**
 * This exception is thrown whenever an invalid {@link RunId} is used.
 */
public class NoSuchRunException extends RuntimeException {

	private final RunId invalidId;

	public NoSuchRunException(RunId invalidId) {
		super("no run with id " + invalidId);
		this.invalidId = invalidId;
	}

	public NoSuchRunException(Throwable t, RunId invalidId) {
		super("no run with id " + invalidId, t);
		this.invalidId = invalidId;
	}

	public RunId getInvalidId() {
		return invalidId;
	}
}
