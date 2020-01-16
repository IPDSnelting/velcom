package de.aaaaaaah.velcom.backend.listener;

import de.aaaaaaah.velcom.backend.access.repo.RepoId;

/**
 * An exception that occurs when trying to search for unknown commits.
 */
public class CommitSearchException extends Exception {

	/**
	 * Constructs a new commit search exception
	 *
	 * @param message describing the cause and context of the exception
	 * @param cause the cause for this exception
	 */
	public CommitSearchException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new commit search exception
	 *
	 * @param repoId the repository that was being searched
	 * @param e the cause for this exception
	 */
	public CommitSearchException(RepoId repoId, Exception e) {
		super("Failed to search for unknown commits in repo: " + repoId, e);
	}

}
