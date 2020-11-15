package de.aaaaaaah.velcom.backend.newaccess.archiveaccess.exceptions;

import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import java.util.Optional;
import javax.annotation.Nullable;

public class TarRetrieveException extends RuntimeException {

	@Nullable
	private final Task task;

	private static String makeMessage(@Nullable Task task) {
		if (task == null) {
			return "Failed to retrieve tar file";
		} else {
			return "Failed to retrieve tar file for task " + task;
		}
	}

	public TarRetrieveException(Throwable t, @Nullable Task task) {
		super(makeMessage(task), t);
		this.task = task;
	}

	public TarRetrieveException(@Nullable Task task) {
		super(makeMessage(task));
		this.task = task;
	}

	public TarRetrieveException() {
		this(null);
	}

	public Optional<Task> getTask() {
		return Optional.ofNullable(task);
	}
}
