package de.aaaaaaah.velcom.backend.access.exceptions;

import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;

/**
 * Thrown when accessing a repository causes an exception.
 */
public class RepoAccessException extends RuntimeException {

	/**
	 * Constructs a new {@link RepoAccessException}.
	 *
	 * @param repoId the id of the repository that was accessed
	 */
	public RepoAccessException(RepoId repoId) {
		super("failed to access repo: " + repoId);
	}

	/**
	 * Constructs a new {@link RepoAccessException}.
	 *
	 * @param repoId the id of the repository that was accessed
	 * @param cause the cause for this exception
	 */
	public RepoAccessException(RepoId repoId, Throwable cause) {
		super("failed to access repo: " + repoId, cause);
	}

	/**
	 * Constructs a new {@link RepoAccessException}.
	 *
	 * @param message message describing the cause for this exception
	 */
	public RepoAccessException(String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link RepoAccessException}.
	 *
	 * @param cause the cause for this exception
	 */
	public RepoAccessException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new {@link RepoAccessException}.
	 *
	 * @param message message describing the cause for this exception
	 * @param cause the cause for this exception
	 */
	public RepoAccessException(String message, Throwable cause) {
		super(message, cause);
	}

}
