package de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities;

import de.aaaaaaah.velcom.backend.access.entities.RunId;
import java.util.Objects;
import java.util.UUID;

/**
 * A unique identifier for a {@link Task}.
 */
public class TaskId {

	private final UUID id;

	public TaskId(UUID id) {
		this.id = Objects.requireNonNull(id);
	}

	/**
	 * Create a new, random {@link TaskId}.
	 */
	public TaskId() {
		this(UUID.randomUUID());
	}

	public static TaskId fromString(String string) {
		return new TaskId(UUID.fromString(string));
	}

	public UUID getId() {
		return id;
	}

	public String getIdAsString() {
		return id.toString();
	}

	public RunId toRunId() {
		return new RunId(id);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TaskId repoId = (TaskId) o;
		return Objects.equals(id, repoId.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "TaskId{" +
			"id=" + id +
			'}';
	}

}
