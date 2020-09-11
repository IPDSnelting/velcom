package de.aaaaaaah.velcom.backend.data.repocomparison;

import static java.util.Objects.requireNonNull;

import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import java.time.Instant;
import java.util.List;

public class RepoComparisonGraph {

	private final DimensionInfo dimensionInfo;
	private final List<RepoGraphData> data;
	private final Instant startTime;
	private final Instant endTime;

	public RepoComparisonGraph(DimensionInfo dimensionInfo,
		List<RepoGraphData> data, Instant startTime, Instant endTime) {
		this.dimensionInfo = requireNonNull(dimensionInfo);
		this.data = requireNonNull(data);
		this.startTime = requireNonNull(startTime);
		this.endTime = requireNonNull(endTime);
	}

	public DimensionInfo getDimensionInfo() {
		return dimensionInfo;
	}

	public List<RepoGraphData> getData() {
		return data;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	@Override
	public String toString() {
		return "RepoComparisonGraph{" +
			"dimensionInfo=" + dimensionInfo +
			", startTime=" + startTime +
			", endTime=" + endTime +
			'}';
	}

}
