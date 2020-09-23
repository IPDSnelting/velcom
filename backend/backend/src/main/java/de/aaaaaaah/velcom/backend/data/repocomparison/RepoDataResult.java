package de.aaaaaaah.velcom.backend.data.repocomparison;

import java.time.Instant;
import java.util.Objects;

/**
 * Contains the comparison graph data for a single repository, the startTime (which was either set
 * manually or is the author date of the oldest commit of this repository in this graph), and the
 * endTime which also was either set or is the author date of the youngest commit of this repository
 * in this graph.
 */
public class RepoDataResult {

	private final RepoGraphData graphData;
	private final Instant startTime;
	private final Instant endTime;

	public RepoDataResult(RepoGraphData graphData, Instant startTime, Instant endTime) {
		this.graphData = Objects.requireNonNull(graphData);
		this.startTime = Objects.requireNonNull(startTime);
		this.endTime = Objects.requireNonNull(endTime);
	}

	public RepoGraphData getGraphData() {
		return graphData;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	@Override
	public String toString() {
		return "RepoDataResult{" +
			"graphData=" + graphData +
			", startTime=" + startTime +
			", endTime=" + endTime +
			'}';
	}

}
