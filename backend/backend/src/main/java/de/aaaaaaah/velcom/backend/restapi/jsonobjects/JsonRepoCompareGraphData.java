package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import java.util.List;

public class JsonRepoCompareGraphData {

	private final RepoId repoId;
	private final List<JsonRepoCompareGraphEntry> commits;

	public JsonRepoCompareGraphData(RepoId repoId,
		List<JsonRepoCompareGraphEntry> commits) {
		this.repoId = repoId;
		this.commits = commits;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public List<JsonRepoCompareGraphEntry> getCommits() {
		return commits;
	}

}
