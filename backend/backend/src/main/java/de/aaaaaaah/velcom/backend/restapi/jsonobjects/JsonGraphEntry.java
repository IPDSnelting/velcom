package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.repocomparison.GraphEntry;

public class JsonGraphEntry {

	private final JsonCommit commit;
	private final double value;

	public JsonGraphEntry(GraphEntry entry) {
		this.commit = new JsonCommit(entry.getCommit());
		this.value = entry.getValue();
	}

	public JsonCommit getCommit() {
		return commit;
	}

	public double getValue() {
		return value;
	}
}
