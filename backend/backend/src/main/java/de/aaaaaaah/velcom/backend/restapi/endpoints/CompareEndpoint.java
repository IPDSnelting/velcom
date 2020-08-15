package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimensionDifference;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for comparing two runs against each other.
 */
@Path("/compare/{runid}/to/{runid}")
@Produces(MediaType.APPLICATION_JSON)
public class CompareEndpoint {

	private final BenchmarkReadAccess benchmarkAccess;

	public CompareEndpoint(BenchmarkReadAccess benchmarkAccess) {
		this.benchmarkAccess = benchmarkAccess;
	}

	@GET
	public GetReply get(
		@QueryParam("all_values") Boolean allValues,
		@QueryParam("hash1") String hash1,
		@QueryParam("hash2") String hash2
	) {
		// TODO get the first and second run either by run id or by repo + hash
		// TODO compare the two runs
		return null;
	}

	private static class GetReply {

		private final JsonRun run1;
		private final JsonRun run2;
		private final List<JsonDimensionDifference> differences;

		public GetReply(JsonRun run1, JsonRun run2, List<JsonDimensionDifference> differences) {
			this.run1 = run1;
			this.run2 = run2;
			this.differences = differences;
		}

		public JsonRun getRun1() {
			return run1;
		}

		public JsonRun getRun2() {
			return run2;
		}

		public List<JsonDimensionDifference> getDifferences() {
			return differences;
		}
	}
}
