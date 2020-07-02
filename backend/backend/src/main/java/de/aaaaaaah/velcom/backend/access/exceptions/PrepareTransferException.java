package de.aaaaaaah.velcom.backend.access.exceptions;

import de.aaaaaaah.velcom.backend.access.entities.Task;

public class PrepareTransferException extends Exception {

	private final Task task;

	public PrepareTransferException(Throwable cause,
		Task task) {
		super(cause);
		this.task = task;
	}

	public Task getTask() {
		return task;
	}

}
