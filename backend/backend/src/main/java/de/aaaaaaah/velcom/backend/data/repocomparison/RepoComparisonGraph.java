package de.aaaaaaah.velcom.backend.data.repocomparison;

import static java.util.Objects.requireNonNull;

import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.DimensionInfo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

public class RepoComparisonGraph {

	private final DimensionInfo dimensionInfo;
	private final List<RepoGraphData> data;
	private final Instant startTime;
	private final Instant endTime;

	public RepoComparisonGraph(DimensionInfo dimensionInfo,
		List<RepoGraphData> data, @Nullable Instant startTime, @Nullable Instant endTime) {
		this.dimensionInfo = requireNonNull(dimensionInfo);
		this.data = requireNonNull(data);
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public DimensionInfo getDimensionInfo() {
		return dimensionInfo;
	}

	public List<RepoGraphData> getData() {
		return data;
	}

	public Optional<Instant> getStartTime() {
		return Optional.ofNullable(startTime);
	}

	public Optional<Instant> getEndTime() {
		return Optional.ofNullable(endTime);
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
