package de.aaaaaaah.velcom.backend.data.repocomparison;

import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.repo.BranchName;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ComparisonGraph {

	private final MeasurementName measurement;
	private final Map<RepoId, List<BranchName>> repoBranches;
	private final Collection<RepoGraphData> data;

	public ComparisonGraph(MeasurementName measurement,
		Map<RepoId, List<BranchName>> repoBranches,
		Collection<RepoGraphData> data) {
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

	public Collection<RepoGraphData> getData() {
		return data;
	}

}
