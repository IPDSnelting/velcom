package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparator;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimensionDifference;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription.JsonSuccess;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import de.aaaaaaah.velcom.shared.util.Pair;
import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for getting a list of the most recent runs.
 */
@Path("/recent/runs")
@Produces(MediaType.APPLICATION_JSON)
public class RecentRunsEndpoint {

	private static final int DEFAULT_N = 10;
	private static final int MIN_N = 1;
	private static final int MAX_N = 100;

	private static final int SIGNIFICANT_MAX_OFFSET = 500;
	private static final int SIGNIFICANT_BATCH_SIZE = 50;

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final RunComparator runComparator;

	public RecentRunsEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		RunComparator runComparator) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.runComparator = runComparator;
	}

	@GET
	@Timed(histogram = true)
	public GetReply getRuns(
		@QueryParam("n") @Nullable Integer nOptional,
		@QueryParam("significant") @Nullable Boolean significantOptional
	) {
		int n = (nOptional == null) ? DEFAULT_N : nOptional;
		n = Math.max(MIN_N, Math.min(MAX_N, n));

		boolean significant = (significantOptional != null) && significantOptional;

		final List<JsonRunEntry> runEntries;
		if (significant) {
			runEntries = getSignificantRuns(n).stream()
				.map(pair -> toJsonRunEntry(pair.getFirst(), pair.getSecond()))
				.collect(Collectors.toList());
		} else {
			runEntries = benchmarkAccess.getRecentRuns(0, n).stream()
				.map(run -> toJsonRunEntry(run, null))
				.collect(Collectors.toList());
		}

		return new GetReply(runEntries);
	}

	private JsonRunEntry toJsonRunEntry(Run run, @Nullable List<DimensionDifference> differences) {
		@Nullable
		List<JsonDimensionDifference> jsonDiffs = null;
		if (differences != null) {
			List<Dimension> dimensions = differences.stream()
				.map(DimensionDifference::getDimension)
				.collect(Collectors.toList());

			Map<Dimension, DimensionInfo> dimInfos = benchmarkAccess.getDimensionInfos(dimensions);

			jsonDiffs = differences.stream()
				.map(diff -> JsonDimensionDifference.fromDimensionDifference(diff, dimInfos))
				.collect(Collectors.toList());
		}

		return new JsonRunEntry(
			new JsonRunDescription(
				run.getId().getId(),
				run.getStartTime().getEpochSecond(),
				JsonSuccess.fromRunResult(run.getResult()),
				JsonSource.fromSource(run.getSource(), commitAccess)
			),
			jsonDiffs
		);
	}

	@Timed(histogram = true)
	private List<Pair<Run, List<DimensionDifference>>> getSignificantRuns(int n) {
		List<Pair<Run, List<DimensionDifference>>> runs = new ArrayList<>();

		outer:
		for (int offset = 0; offset < SIGNIFICANT_MAX_OFFSET; offset += SIGNIFICANT_BATCH_SIZE) {
			List<Run> recentRuns = benchmarkAccess.getRecentRuns(offset, SIGNIFICANT_BATCH_SIZE);
			if (recentRuns.isEmpty()) {
				break;
			}

			for (Run run : recentRuns) {
				List<DimensionDifference> dimensions = getSignificantDimensions(run);
				if (!dimensions.isEmpty()) {
					runs.add(new Pair<>(run, dimensions));

					if (runs.size() >= n) {
						break outer;
					}
				}
			}
		}

		Collections.reverse(runs);
		return runs;
	}

	private List<DimensionDifference> getSignificantDimensions(Run run) {
		if (run.getSource().getLeft().isEmpty()) {
			return List.of();
		}

		CommitSource source = run.getSource().getLeft().get();
		Commit sourceCommit = commitAccess.getCommit(source.getRepoId(), source.getHash());

		return benchmarkAccess
			.getLatestRuns(sourceCommit.getRepoId(), sourceCommit.getParentHashes())
			.values()
			.stream()
			.map(parentRun -> runComparator.compare(parentRun, run))
			.flatMap(comparison -> comparison.getDifferences().stream())
			.filter(DimensionDifference::isSignificant)
			.collect(Collectors.toList());
	}

	private static class GetReply {

		private final List<JsonRunEntry> runs;

		public GetReply(List<JsonRunEntry> runs) {
			this.runs = runs;
		}

		public List<JsonRunEntry> getRuns() {
			return runs;
		}
	}

	private static class JsonRunEntry {

		private final JsonRunDescription run;
		@Nullable
		private final List<JsonDimensionDifference> significantDimensions;

		public JsonRunEntry(JsonRunDescription run,
			@Nullable List<JsonDimensionDifference> significantDimensions) {

			this.run = run;
			this.significantDimensions = significantDimensions;
		}

		public JsonRunDescription getRun() {
			return run;
		}

		@Nullable
		public List<JsonDimensionDifference> getSignificantDimensions() {
			return significantDimensions;
		}
	}
}
