package de.aaaaaaah.velcom.backend.runner;

import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.shared.protocol.serialization.Status;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A runner that is known to the dispatcher.
 */
public class KnownRunner {

	private final String name;
	private final String information;
	private final Status status;
	private final Task currentTask;

	/**
	 * Creates a new known runner.
	 *
	 * @param name the name of the runner
	 * @param information the runner information
	 * @param status the runner state
	 * @param task the task the runner is currently working on
	 */
	public KnownRunner(String name, String information, Status status, @Nullable Task task) {
		this.name = Objects.requireNonNull(name, "name can not be null!");
		this.information = Objects.requireNonNull(information, "information can not be null!");
		this.status = Objects.requireNonNull(status, "status can not be null!");
		this.currentTask = task;
	}

	public String getName() {
		return name;
	}

	public String getInformation() {
		return information;
	}

	public Status getStatus() {
		return status;
	}

	public Optional<Task> getCurrentTask() {
		return Optional.ofNullable(currentTask);
	}
}
