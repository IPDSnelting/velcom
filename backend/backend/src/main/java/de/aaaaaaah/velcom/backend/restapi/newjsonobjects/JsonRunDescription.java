package de.aaaaaaah.velcom.backend.restapi.newjsonobjects;

import java.util.UUID;

public class JsonRunDescription {

	private final UUID id;
	private final long startTime;
	private final JsonSuccess success;

	public JsonRunDescription(UUID id, long startTime, JsonSuccess success) {
		this.id = id;
		this.startTime = startTime;
		this.success = success;
	}

	public UUID getId() {
		return id;
	}

	public long getStartTime() {
		return startTime;
	}

	public JsonSuccess getSuccess() {
		return success;
	}

	public enum JsonSuccess {
		SUCCESS, PARTIAL_SUCCESS, FAILURE
	}
}
