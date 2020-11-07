package de.aaaaaaah.velcom.backend.newaccess.taskaccess.exceptions;

import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;

/**
 * This exception is thrown whenever an invalid {@link TaskId} is used.
 */
public class NoSuchTaskException extends RuntimeException {

	private final TaskId invalidId;

	public NoSuchTaskException(Throwable t, TaskId invalidId) {
		super("no task with id " + invalidId, t);
		this.invalidId = invalidId;
	}

	public TaskId getInvalidId() {
		return invalidId;
	}
}
