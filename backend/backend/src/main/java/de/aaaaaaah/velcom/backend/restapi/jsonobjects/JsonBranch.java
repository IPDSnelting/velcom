package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.repoaccess.entities.Branch;

public class JsonBranch {

	private final String name;
	private final boolean tracked;
	private final String latest_commit;

	public JsonBranch(String name, boolean tracked, String latest_commit) {
		this.name = name;
		this.tracked = tracked;
		this.latest_commit = latest_commit;
	}

	public static JsonBranch fromBranch(Branch branch) {
		return new JsonBranch(
			branch.getName().getName(),
			branch.isTracked(),
			branch.getLatestCommitHash().getHash()
		);
	}

	public String getName() {
		return name;
	}

	public boolean isTracked() {
		return tracked;
	}

	public String getLatest_commit() {
		return latest_commit;
	}
}
