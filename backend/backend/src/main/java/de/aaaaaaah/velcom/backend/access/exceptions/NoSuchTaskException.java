package de.aaaaaaah.velcom.backend.access.exceptions;

import de.aaaaaaah.velcom.backend.access.entities.TaskId;

public class NoSuchTaskException extends Exception {

	private final TaskId taskId;

	public NoSuchTaskException(TaskId taskId) {
		this.taskId = taskId;
	}

	public TaskId getInvalidId() {
		return taskId;
	}
}
