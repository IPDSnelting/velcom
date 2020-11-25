package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparator;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparison;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.restapi.endpoints.utils.EndpointUtils;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimensionDifference;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for comparing two runs against each other.
 */
@Path("/compare/{runid1}/to/{runid2}")
@Produces(MediaType.APPLICATION_JSON)
public class CompareEndpoint {

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final RunComparator comparer;

	public CompareEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		RunComparator comparer) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.comparer = comparer;
	}

	@GET
	@Timed(histogram = true)
	public GetReply get(
		@PathParam("runid1") UUID runUuid1,
		@PathParam("runid2") UUID runUuid2,
		@QueryParam("hash1") @Nullable String hash1,
		@QueryParam("hash2") @Nullable String hash2,
		@QueryParam("all_values") @Nullable Boolean allValuesOptional
	) throws NoSuchRunException {
		boolean allValues = (allValuesOptional != null) && allValuesOptional;

		Run run1 = EndpointUtils.getRun(benchmarkAccess, runUuid1, hash1);
		Run run2 = EndpointUtils.getRun(benchmarkAccess, runUuid2, hash2);

		RunComparison comparison = comparer.compare(run1, run2);

		Map<Dimension, DimensionInfo> infos = benchmarkAccess
			.getDimensionInfos(comparison.getDimensions());

		List<JsonDimensionDifference> differences = JsonDimensionDifference
			.fromRunComparison(comparison, infos);

		return new GetReply(
			EndpointUtils.fromRun(benchmarkAccess, commitAccess, run1, allValues),
			EndpointUtils.fromRun(benchmarkAccess, commitAccess, run2, allValues),
			differences
		);
	}

	private static class GetReply {

		public final JsonRun run1;
		public final JsonRun run2;
		public final List<JsonDimensionDifference> differences;

		public GetReply(JsonRun run1, JsonRun run2, List<JsonDimensionDifference> differences) {
			this.run1 = run1;
			this.run2 = run2;
			this.differences = differences;
		}
	}
}
