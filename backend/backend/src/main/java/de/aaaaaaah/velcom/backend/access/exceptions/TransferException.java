package de.aaaaaaah.velcom.backend.access.exceptions;

import de.aaaaaaah.velcom.backend.access.entities.Task;

public class TransferException extends Exception {

	public TransferException(Task task, Throwable cause) {
		super("failed transfer task: " + task, cause);
	}

	public TransferException(String message, Throwable cause) {
		super(message, cause);
	}

}
