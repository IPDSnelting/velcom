package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import java.util.UUID;

public class JsonTask {

	private final UUID id;
	private final String author;
	private final long since;
	private final JsonSource source;

	public JsonTask(UUID id, String author, long since, JsonSource source) {
		this.id = id;
		this.author = author;
		this.since = since;
		this.source = source;
	}

	public UUID getId() {
		return id;
	}

	public String getAuthor() {
		return author;
	}

	public long getSince() {
		return since;
	}

	public JsonSource getSource() {
		return source;
	}
}
