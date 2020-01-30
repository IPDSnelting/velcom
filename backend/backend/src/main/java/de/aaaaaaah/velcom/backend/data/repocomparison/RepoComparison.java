package de.aaaaaaah.velcom.backend.data.repocomparison;

import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.repo.BranchName;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public interface RepoComparison {

	ComparisonGraph generateGraph(MeasurementName measurement,
		Map<RepoId, List<BranchName>> branches,
		@Nullable Instant startTime, @Nullable Instant stopTime);

}
