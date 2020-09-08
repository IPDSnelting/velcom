package de.aaaaaaah.velcom.backend.restapi.exception;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;

/**
 * Thrown when a task already exists in the queue but is added again.
 */
public class TaskAlreadyExistsException extends RuntimeException {

	private final CommitHash hash;
	private final RepoId repoId;
	private final TaskId taskId;

	public TaskAlreadyExistsException(CommitHash hash, RepoId repoId, TaskId taskId) {
		super("task already exists");
		this.hash = hash;
		this.repoId = repoId;
		this.taskId = taskId;
	}

	public CommitHash getHash() {
		return hash;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public TaskId getTaskId() {
		return taskId;
	}
}
