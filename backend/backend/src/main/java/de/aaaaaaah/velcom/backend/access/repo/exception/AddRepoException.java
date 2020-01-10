package de.aaaaaaah.velcom.backend.access.repo.exception;

/**
 * Throws when a {@link de.aaaaaaah.velcom.backend.access.repo.Repo} could not be added.
 */
public class AddRepoException extends RuntimeException {

	public AddRepoException(Throwable cause) {
		super(cause);
	}

}
