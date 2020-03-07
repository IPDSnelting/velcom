package de.aaaaaaah.velcom.backend.data.repocomparison;

import de.aaaaaaah.velcom.backend.newaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ComparisonGraph {

	private final MeasurementName measurement;
	private final Map<RepoId, List<BranchName>> repoBranches;
	private final List<RepoGraphData> data;

	public ComparisonGraph(MeasurementName measurement,
		Map<RepoId, List<BranchName>> repoBranches,
		List<RepoGraphData> data) {
		this.measurement = measurement;
		this.repoBranches = repoBranches;
		this.data = data;
	}

	public MeasurementName getMeasurement() {
		return measurement;
	}

	public Map<RepoId, List<BranchName>> getRepoBranches() {
		return repoBranches;
	}

	public List<RepoGraphData> getData() {
		return data;
	}

}
