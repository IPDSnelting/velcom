package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.repocomparison.TmpEntry;

public class JsonGraphEntry {

	private final JsonCommit commit;
	private final double value;

	public JsonGraphEntry(TmpEntry entry) {
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
