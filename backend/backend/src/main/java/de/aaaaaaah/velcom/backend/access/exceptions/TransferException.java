package de.aaaaaaah.velcom.backend.access.exceptions;

import de.aaaaaaah.velcom.backend.access.entities.Task;

public class TransferException extends Exception {

	private final Task task;

	public TransferException(Throwable cause, Task task) {
		super(cause);
		this.task = task;
	}

	public Task getTask() {
		return task;
	}

}
