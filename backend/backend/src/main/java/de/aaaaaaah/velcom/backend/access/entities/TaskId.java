package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Objects;
import java.util.UUID;

public class TaskId {

	private final UUID id;

	public TaskId(UUID id) {
		this.id = Objects.requireNonNull(id);
	}

	public TaskId() {
		this(UUID.randomUUID());
	}

	public UUID getId() {
		return id;
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
