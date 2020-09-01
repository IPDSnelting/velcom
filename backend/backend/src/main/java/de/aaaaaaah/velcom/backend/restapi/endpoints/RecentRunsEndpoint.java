package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.restapi.endpoints.utils.EndpointUtils;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimensionDifference;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRunDescription.JsonSuccess;
import java.util.List;
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

	public RecentRunsEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess) {
		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
	}

	@GET
	public GetReply getRuns(
		@QueryParam("n") @Nullable Integer nOptional,
		@QueryParam("significant") @Nullable Boolean significantOptional
	) {
		int n = (nOptional == null) ? DEFAULT_N : nOptional;
		n = Math.max(MIN_N, Math.min(MAX_N, n));

		boolean significant = (significantOptional != null) && significantOptional;

		if (significant) {
			// TODO: 01.09.20 Implement getting significant runs
			return new GetReply(List.of());
		}

		List<JsonRunEntry> recentRuns = benchmarkAccess.getRecentRuns(0, n).stream()
			.map(run -> new JsonRunEntry(
				new JsonRunDescription(
					run.getId().getId(),
					run.getStartTime().getEpochSecond(),
					JsonSuccess.fromRunResult(run.getResult()),
					EndpointUtils.convertToSource(commitAccess, run.getSource())
				),
				null
			))
			.collect(Collectors.toList());

		return new GetReply(recentRuns);
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
