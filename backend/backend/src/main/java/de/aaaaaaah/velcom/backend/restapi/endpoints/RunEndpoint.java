package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimensionDifference;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonResult;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/run/{runid}")
@Produces(MediaType.APPLICATION_JSON)
public class RunEndpoint {

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;

	public RunEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess) {
		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
	}

	@GET
	public GetReply get(
		@PathParam("runid") UUID runUuid,
		@QueryParam("all_values") @Nullable Boolean allValuesOptional,
		@QueryParam("hash") @Nullable String hashString,
		@QueryParam("diff_prev") @Nullable Boolean diffPrevOptional
	) {
		boolean allValues = (allValuesOptional == null) ? false : allValuesOptional;
		boolean diffPrev = (diffPrevOptional == null) ? false : diffPrevOptional;

		Run run = EndpointUtils.getRun(benchmarkAccess, runUuid, hashString);
		JsonSource source = EndpointUtils.convertToSource(commitAccess, run.getSource());

		// TODO find previous run and compare
		return new GetReply(
			new JsonRun(
				run.getId().getId(),
				run.getAuthor(),
				run.getRunnerName(),
				run.getRunnerInfo(),
				run.getStartTime().getEpochSecond(),
				run.getStopTime().getEpochSecond(),
				source,
				JsonResult.fromRunResult(run.getResult(), allValues)
			),
			// TODO use actual comparison to previous run, if applicable
			null
		);
	}

	private static class GetReply {

		public final JsonRun run;
		@Nullable
		public final List<JsonDimensionDifference> differences;

		public GetReply(JsonRun run, @Nullable List<JsonDimensionDifference> differences) {
			this.run = run;
			this.differences = differences;
		}
	}
}
