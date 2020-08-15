package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import java.util.UUID;

public class JsonRunDescription {

	private final UUID id;
	private final long startTime;
	private final JsonSuccess success;
	private final JsonSource source;

	public JsonRunDescription(UUID id, long startTime, JsonSuccess success, JsonSource source) {
		this.id = id;
		this.startTime = startTime;
		this.success = success;
		this.source = source;
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

	public JsonSource getSource() {
		return source;
	}

	public enum JsonSuccess {
		SUCCESS, PARTIAL_SUCCESS, FAILURE
	}
}
