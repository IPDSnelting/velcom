package de.aaaaaaah.velcom.backend.access.archiveaccess.exceptions;

import de.aaaaaaah.velcom.backend.access.taskaccess.entities.Task;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Thrown when something goes wrong during the transfer of a tar file.
 */
public class TarTransferException extends Exception {

	@Nullable
	private final Task task;

	private static String makeMessage(@Nullable Task task) {
		if (task == null) {
			return "Failed to transfer tar file";
		} else {
			return "Failed to transfer tar file for task " + task;
		}
	}

	public TarTransferException(Throwable t, @Nullable Task task) {
		super(makeMessage(task), t);
		this.task = task;
	}

	public TarTransferException(@Nullable Task task) {
		super(makeMessage(task));
		this.task = task;
	}

	public TarTransferException() {
		this(null);
	}

	public Optional<Task> getTask() {
		return Optional.ofNullable(task);
	}
}
