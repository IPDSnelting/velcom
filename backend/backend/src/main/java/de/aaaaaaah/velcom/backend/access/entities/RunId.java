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

	/**
	 * Create a new {@link RunId} from a UUID string.
	 *
	 * @param string the UUID as string
	 * @return the run id
	 */
	public static RunId fromString(String string) {
		return new RunId(UUID.fromString(string));
	}

	public UUID getId() {
		return id;
	}

	public String getIdAsString() {
		return id.toString();
	}

	/**
	 * Convert a run id to a task id.
	 *
	 * @return a task id with the same UUID
	 */
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
