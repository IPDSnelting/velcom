package de.aaaaaaah.velcom.backend.data.repocomparison;

import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;

public class GraphEntry {

	private final Commit commit;
	private final double value;

	public GraphEntry(Commit commit, double value) {
		this.commit = commit;
		this.value = value;
	}

	public Commit getCommit() {
		return commit;
	}

	public double getValue() {
		return value;
	}
}
