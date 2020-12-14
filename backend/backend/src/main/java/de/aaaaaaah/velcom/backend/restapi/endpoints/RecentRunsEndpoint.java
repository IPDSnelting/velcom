package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.data.recentruns.SignificantRunsCollector;
import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimensionDifference;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription.JsonSuccess;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final DimensionReadAccess dimensionAccess;
	private final SignificantRunsCollector significantRunsCollector;

	public RecentRunsEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		DimensionReadAccess dimensionAccess, SignificantRunsCollector significantRunsCollector) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.dimensionAccess = dimensionAccess;
		this.significantRunsCollector = significantRunsCollector;
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
			runEntries = significantRunsCollector.collectMostRecent(n).stream()
				.map(run -> toJsonRunEntry(run.getRun(), run.getSignificantDifferences()))
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
			Set<Dimension> dimensions = differences.stream()
				.map(DimensionDifference::getDimension)
				.collect(Collectors.toSet());

			Map<Dimension, DimensionInfo> dimInfos = dimensionAccess.getDimensionInfoMap(dimensions);

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
