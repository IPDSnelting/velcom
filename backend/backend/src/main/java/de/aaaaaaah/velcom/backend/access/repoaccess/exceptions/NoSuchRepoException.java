package de.aaaaaaah.velcom.backend.access.repoaccess.exceptions;

import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;

/**
 * This exception is thrown whenever an invalid {@link RepoId} is used.
 */
public class NoSuchRepoException extends RuntimeException {

	private final RepoId invalidId;

	public NoSuchRepoException(Throwable t, RepoId invalidId) {
		super("no repo with id " + invalidId, t);
		this.invalidId = invalidId;
	}

	public RepoId getInvalidId() {
		return invalidId;
	}
}
