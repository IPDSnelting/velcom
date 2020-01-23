package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.data.commitcomparison.CommitComparison;

public class JsonCommitHistoryEntry {

	private final JsonCommit commit;
	private final JsonCommitComparison comparison;

	public JsonCommitHistoryEntry(Commit commit, CommitComparison comparison) {
		this.commit = new JsonCommit(commit);
		this.comparison = new JsonCommitComparison(comparison);
	}

	public JsonCommit getCommit() {
		return commit;
	}

	public JsonCommitComparison getComparison() {
		return comparison;
	}
}
