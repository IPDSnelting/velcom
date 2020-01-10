package de.aaaaaaah.designproto.backend.access.repo.exception;

import de.aaaaaaah.designproto.backend.access.repo.RepoId;

/**
 * Thrown when accessing a repository causes an exception.
 */
public class RepoAccessException extends RuntimeException {

	private final RepoId repoId;

	/**
	 * Constructs a new {@link RepoAccessException}.
	 *
	 * @param repoId the id of the repository that was accessed
	 */
	public RepoAccessException(RepoId repoId) {
		this.repoId = repoId;
	}

	/**
	 * @return the id of the repository that was accessed
	 */
	public RepoId getRepoId() {
		return repoId;
	}

}
