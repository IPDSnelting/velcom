package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.entities.Unit;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import java.util.List;


public class JsonGraphRepoInfo {

	private final String repoId;
	private final List<JsonGraphEntry> commits;
	private final String interpretation;
	private final String unit;

	public JsonGraphRepoInfo(RepoId repoId, List<JsonGraphEntry> entries,
		Interpretation interpretation, Unit unit) {

		this.repoId = repoId.getId().toString();
		this.commits = entries;
		this.interpretation = interpretation.getTextualRepresentation();
		this.unit = unit.getName();
	}

	public String getRepoId() {
		return repoId;
	}

	public List<JsonGraphEntry> getCommits() {
		return commits;
	}

	public String getInterpretation() {
		return interpretation;
	}

	public String getUnit() {
		return unit;
	}
}