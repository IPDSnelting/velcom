package de.aaaaaaah.velcom.backend.runner;

import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.shared.protocol.serialization.Status;
import de.aaaaaaah.velcom.shared.util.LinesWithOffset;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * A runner that is known to the dispatcher.
 */
public class KnownRunner {

	private final String name;
	private final String information;
	@Nullable
	private final String versionHash;
	private final Status lastStatus;
	private final boolean lostConnection;
	@Nullable
	private final Task currentTask;
	@Nullable
	final LinesWithOffset lastOutputLines;
	@Nullable
	private final Instant workingSince;

	/**
	 * Creates a new known runner.
	 *
	 * @param name the name of the runner
	 * @param information the runner information
	 * @param versionHash the commit hash the runner was built on
	 * @param lastStatus the last known runner status
	 * @param task the task the runner is currently working on
	 * @param lostConnection true if the connection to the runner is lost
	 * @param workingSince the time the runner is working on a run now
	 * @param lastOutputLines the last output lines
	 */
	public KnownRunner(String name, String information, @Nullable String versionHash,
		Status lastStatus, @Nullable Task task, boolean lostConnection,
		@Nullable Instant workingSince, @Nullable LinesWithOffset lastOutputLines) {
		this.name = Objects.requireNonNull(name, "name can not be null!");
		this.information = Objects.requireNonNull(information, "information can not be null!");
		this.versionHash = versionHash;
		this.lastStatus = Objects.requireNonNull(lastStatus, "status can not be null!");
		this.currentTask = task;
		this.lostConnection = lostConnection;
		this.workingSince = workingSince;
		this.lastOutputLines = lastOutputLines;
	}

	public String getName() {
		return name;
	}

	public String getInformation() {
		return information;
	}

	public Optional<String> getVersionHash() {
		return Optional.ofNullable(versionHash);
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

	/**
	 * @return the last output lines
	 */
	public Optional<LinesWithOffset> getLastOutputLines() {
		return Optional.ofNullable(lastOutputLines);
	}

	/**
	 * @return the duration since the runner started working on a run. Includes transfer time.
	 */
	public Optional<Instant> getWorkingSince() {
		return Optional.ofNullable(workingSince);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		KnownRunner that = (KnownRunner) o;
		return lostConnection == that.lostConnection &&
			Objects.equals(name, that.name) &&
			Objects.equals(information, that.information) &&
			Objects.equals(versionHash, that.versionHash) &&
			lastStatus == that.lastStatus &&
			Objects.equals(currentTask, that.currentTask) &&
			Objects.equals(workingSince, that.workingSince);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			name, information, versionHash, lastStatus, lostConnection, currentTask, workingSince
		);
	}

	@Override
	public String toString() {
		return "KnownRunner{" +
			"name='" + name + '\'' +
			", information='" + information + '\'' +
			", versionHash='" + versionHash + '\'' +
			", lastStatus=" + lastStatus +
			", lostConnection=" + lostConnection +
			", currentTask=" + currentTask +
			", workingSince=" + workingSince +
			'}';
	}
}
