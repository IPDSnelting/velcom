package de.aaaaaaah.velcom.backend.access.exceptions;

import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import java.util.NoSuchElementException;

/**
 * This exception is thrown whenever an invalid {@link RunId} is used.
 */
public class NoSuchRunException extends NoSuchElementException {

	private final RunId invalidId;

	public NoSuchRunException(RunId invalidId) {
		this.invalidId = invalidId;
	}

	public RunId getInvalidId() {
		return invalidId;
	}

	@Override
	public String getMessage() {
		return "no run with id " + invalidId;
	}
}
