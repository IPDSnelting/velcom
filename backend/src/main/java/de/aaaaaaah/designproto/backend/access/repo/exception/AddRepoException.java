package de.aaaaaaah.designproto.backend.access.repo.exception;

/**
 * Throws when a {@link de.aaaaaaah.designproto.backend.access.repo.Repo} could not be added.
 */
public class AddRepoException extends RuntimeException {

	public AddRepoException(Throwable cause) {
		super(cause);
	}

}
