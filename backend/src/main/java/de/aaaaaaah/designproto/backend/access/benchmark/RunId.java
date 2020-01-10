package de.aaaaaaah.designproto.backend.access.benchmark;

import java.util.Objects;
import java.util.UUID;

/**
 * A unique identifier for a {@link Run}.
 */
public class RunId {

	private final UUID id;

	public RunId(UUID id) {
		this.id = id;
	}

	/**
	 * Create a new, random {@link RunId}.
	 */
	public RunId() {
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
		RunId runId = (RunId) o;
		return id.equals(runId.id);
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
