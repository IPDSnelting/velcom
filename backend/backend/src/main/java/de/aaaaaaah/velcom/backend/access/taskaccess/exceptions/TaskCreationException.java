package de.aaaaaaah.velcom.backend.access.taskaccess.exceptions;

public class TaskCreationException extends Exception {

	private final boolean ourFault;

	public TaskCreationException(String message, boolean ourFault) {
		super(message);

		this.ourFault = ourFault;
	}

	public boolean isOurFault() {
		return ourFault;
	}
}
