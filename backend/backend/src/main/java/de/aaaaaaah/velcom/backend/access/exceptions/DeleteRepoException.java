package de.aaaaaaah.velcom.backend.access.exceptions;

import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;

/**
 * Thrown when a repo could not be deleted.
 */
public class DeleteRepoException extends RuntimeException {

	private final RepoId repoId;

	public DeleteRepoException(RepoId repoId, Throwable cause) {
		super("failed to delete repo: " + repoId, cause);
		this.repoId = repoId;
	}

	public RepoId getRepoId() {
		return repoId;
	}

}
