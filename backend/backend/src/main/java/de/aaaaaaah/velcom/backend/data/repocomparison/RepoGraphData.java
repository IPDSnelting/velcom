package de.aaaaaaah.velcom.backend.data.repocomparison;

import de.aaaaaaah.velcom.backend.newaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.newaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import java.util.List;

public class RepoGraphData {

	private final RepoId repoId;
	private final List<GraphEntry> entries;
	private final Interpretation interpretation;
	private final Unit unit;

	public RepoGraphData(RepoId repoId,
		List<GraphEntry> entries,
		Interpretation interpretation, Unit unit) {
		this.repoId = repoId;
		this.entries = entries;
		this.interpretation = interpretation;
		this.unit = unit;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public List<GraphEntry> getEntries() {
		return entries;
	}

	public Interpretation getInterpretation() {
		return interpretation;
	}

	public Unit getUnit() {
		return unit;
	}
}
