package de.aaaaaaah.velcom.backend.access.exceptions;


import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;

/**
 * An exception that occurs when the preparation of a task transfer has failed.
 */
public class PrepareTransferException extends Exception {

	public PrepareTransferException(Task task, Throwable cause) {
		super("failed to prepare transfer for task: " + task, cause);
	}

	public PrepareTransferException(String message, Throwable cause) {
		super(message, cause);
	}

	public PrepareTransferException(String message) {
		super(message);
	}

}
