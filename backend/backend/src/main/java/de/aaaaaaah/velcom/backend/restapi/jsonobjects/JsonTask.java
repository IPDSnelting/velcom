package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Task;
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

	public static JsonTask fromTask(Task task, JsonSource source) {
		return new JsonTask(
			task.getId().getId(),
			task.getAuthor(),
			task.getInsertTime().getEpochSecond(),
			source
		);
	}

	public static JsonTask fromTask(Task task, CommitReadAccess commitAccess) {
		return fromTask(task, task.getSource()
			.mapLeft(it -> commitAccess.getCommit(it.getRepoId(), it.getHash()))
			.consume(JsonSource::fromCommit, JsonSource::fromTarSource));
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
