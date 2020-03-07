package de.aaaaaaah.velcom.backend.access.exceptions;

import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import java.util.NoSuchElementException;

/**
 * This exception is thrown whenever an invalid {@link RepoId} is used.
 */
public class NoSuchRepoException extends NoSuchElementException {

	private final RepoId invalidId;

	public NoSuchRepoException(RepoId invalidId) {
		this.invalidId = invalidId;
	}

	public RepoId getInvalidId() {
		return invalidId;
	}

	@Override
	public String getMessage() {
		return "no repo with id " + invalidId;
	}
}
