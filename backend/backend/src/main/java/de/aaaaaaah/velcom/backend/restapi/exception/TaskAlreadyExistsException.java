package de.aaaaaaah.velcom.backend.restapi.exception;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;

/**
 * Thrown when a task already exists in the queue but is added again.
 */
public class TaskAlreadyExistsException extends RuntimeException {

	private final CommitHash hash;
	private final RepoId repoId;

	public TaskAlreadyExistsException(CommitHash hash, RepoId repoId) {
		super("task already exists");
		this.hash = hash;
		this.repoId = repoId;
	}

	public CommitHash getHash() {
		return hash;
	}

	public RepoId getRepoId() {
		return repoId;
	}
}
