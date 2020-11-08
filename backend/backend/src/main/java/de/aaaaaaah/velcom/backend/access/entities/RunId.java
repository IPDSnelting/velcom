package de.aaaaaaah.velcom.backend.access.entities;

import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import java.util.Objects;
import java.util.UUID;

/**
 * A unique identifier for a {@link Run}.
 */
public class RunId {

	private final UUID id;

	public RunId(UUID id) {
		this.id = Objects.requireNonNull(id);
	}

	/**
	 * Create a new, random {@link RunId}.
	 */
	public RunId() {
		this(UUID.randomUUID());
	}

	public static RunId fromString(String string) {
		return new RunId(UUID.fromString(string));
	}

	public UUID getId() {
		return id;
	}

	public String getIdAsString() {
		return id.toString();
	}

	public TaskId toTaskId() {
		return new TaskId(id);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RunId runId = (RunId) o;
		return Objects.equals(id, runId.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "RunId{" +
			"id=" + id +
			'}';
	}

}
