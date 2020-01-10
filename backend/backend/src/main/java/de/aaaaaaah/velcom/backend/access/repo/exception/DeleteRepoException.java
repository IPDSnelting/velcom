package de.aaaaaaah.velcom.backend.access.repo.exception;

/**
 * Thrown when a {@link de.aaaaaaah.velcom.backend.access.repo.Repo} could not be deleted.
 */
public class DeleteRepoException extends RuntimeException {

	public DeleteRepoException(Throwable cause) {
		super(cause);
	}

}
