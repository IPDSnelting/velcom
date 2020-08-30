package de.aaaaaaah.velcom.backend.data.repocomparison;

import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import java.util.List;
import java.util.Map;

public class ComparisonGraph {

	private final Dimension measurement;
	private final Map<RepoId, List<BranchName>> repoBranches;
	private final List<RepoGraphData> data;

	public ComparisonGraph(Dimension measurement,
		Map<RepoId, List<BranchName>> repoBranches,
		List<RepoGraphData> data) {
		this.measurement = measurement;
		this.repoBranches = repoBranches;
		this.data = data;
	}

	public Dimension getMeasurement() {
		return measurement;
	}

	public Map<RepoId, List<BranchName>> getRepoBranches() {
		return repoBranches;
	}

	public List<RepoGraphData> getData() {
		return data;
	}

}
