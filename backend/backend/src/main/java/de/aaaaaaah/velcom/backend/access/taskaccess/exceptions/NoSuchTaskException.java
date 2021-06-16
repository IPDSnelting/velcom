package de.aaaaaaah.velcom.backend.access.taskaccess.exceptions;

import de.aaaaaaah.velcom.backend.access.taskaccess.entities.TaskId;

/**
 * This exception is thrown whenever an invalid {@link TaskId} is used.
 */
public class NoSuchTaskException extends RuntimeException {

	private final TaskId invalidId;

	public NoSuchTaskException(TaskId invalidId) {
		super("no task with id " + invalidId);
		this.invalidId = invalidId;
	}

	public NoSuchTaskException(Throwable t, TaskId invalidId) {
		super("no task with id " + invalidId, t);
		this.invalidId = invalidId;
	}

	public TaskId getInvalidId() {
		return invalidId;
	}
}
