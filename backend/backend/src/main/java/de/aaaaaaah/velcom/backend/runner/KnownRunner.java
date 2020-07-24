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
	private final Status lastStatus;
	private final Task currentTask;
	private final boolean lostConnection;

	/**
	 * Creates a new known runner.
	 *
	 * @param name the name of the runner
	 * @param information the runner information
	 * @param lastStatus the las known runner status
	 * @param task the task the runner is currently working on
	 * @param lostConnection true if the connection to the runner is lost
	 */
	public KnownRunner(String name, String information, Status lastStatus, @Nullable Task task,
		boolean lostConnection) {
		this.name = Objects.requireNonNull(name, "name can not be null!");
		this.information = Objects.requireNonNull(information, "information can not be null!");
		this.lastStatus = Objects.requireNonNull(lastStatus, "status can not be null!");
		this.currentTask = task;
		this.lostConnection = lostConnection;
	}

	public String getName() {
		return name;
	}

	public String getInformation() {
		return information;
	}

	/**
	 * @return the last known status. Might be out of date if {@link #hasLostConnection()} is true
	 */
	public Status getLastStatus() {
		return lastStatus;
	}

	public Optional<Task> getCurrentTask() {
		return Optional.ofNullable(currentTask);
	}

	public boolean hasLostConnection() {
		return lostConnection;
	}
}
