package de.aaaaaaah.velcom.backend.data.repocomparison;

import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Can generate a comparison graph showing how certain repositories perform regarding a certain
 * measurement.
 */
public interface RepoComparison {

	/**
	 * Generates a comparison graph using the given measurement to determine how well repositories
	 * are doing. A startTime and stopTime can be provided to limit the graph to a certain time
	 * frame.
	 *
	 * @param measurement the measurement to use for comparison
	 * @param repoBranches the repositories and which of their branches should be considered in
	 * 	this comparsion
	 * @param startTime the instant before which commits won't be considered
	 * @param stopTime the instant after which commits won't be considered
	 * @return the generated graph
	 */
	RepoComparisonGraph generateGraph(Dimension measurement,
		Map<RepoId, Set<BranchName>> repoBranches,
		@Nullable Instant startTime, @Nullable Instant stopTime);

}
