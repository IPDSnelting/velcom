package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.data.repocomparison.RepoComparison;
import de.aaaaaaah.velcom.backend.data.repocomparison.RepoComparisonGraph;
import de.aaaaaaah.velcom.backend.restapi.endpoints.utils.EndpointUtils;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRepoCompareGraphData;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRepoCompareGraphEntry;
import de.aaaaaaah.velcom.backend.util.Pair;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for getting a list of the most recent runs.
 */
@Path("/graph/comparison")
@Produces(MediaType.APPLICATION_JSON)
public class RepoComparisonEndpoint {

	private final RepoComparison comparison;
	private final BenchmarkReadAccess benchmarkAccess;

	public RepoComparisonEndpoint(RepoComparison comparison, BenchmarkReadAccess benchmarkAccess) {
		this.comparison = comparison;
		this.benchmarkAccess = benchmarkAccess;
	}

	@GET
	public GetReply getRuns(
		@QueryParam("repos") String reposStr,
		@QueryParam("start_time") @Nullable Long startTimeEpoch,
		@QueryParam("end_time") @Nullable Long endTimeEpoch,
		@QueryParam("duration") @Nullable Integer durationInSeconds,
		@QueryParam("dimension") String dimensionStr
	) {

		// Parse dimension
		Set<Dimension> dimensionSet = EndpointUtils.parseDimensions(dimensionStr);
		if (dimensionSet.size() != 1) {
			throw new ForbiddenException("invalid amount of dimensions provided");
		}

		Dimension dimension = dimensionSet.iterator().next();

		if (!benchmarkAccess.doesDimensionExist(dimension)) {
			throw new NotFoundException("unknown dimension");
		}

		// Parse startTime, endTime and duration
		Instant startTime = startTimeEpoch == null ? null : Instant.ofEpochSecond(startTimeEpoch);
		Instant endTime = endTimeEpoch == null ? null : Instant.ofEpochSecond(endTimeEpoch);
		Duration duration = durationInSeconds == null ? null : Duration.ofSeconds(durationInSeconds);

		if (duration != null) {
			if (startTime == null && endTime == null) {
				startTime = Instant.now().minus(duration);
				endTime = Instant.now();
			} else if (startTime != null && endTime == null) {
				endTime = startTime.plus(duration);
			} else if (startTime == null && endTime != null) {
				startTime = endTime.minus(duration);
			} else {
				throw new ForbiddenException("duration and startTime and endTime provided");
			}
		}

		if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
			throw new IllegalStateException();
		}

		// Parse repos
		final Map<RepoId, Set<BranchName>> repos = EndpointUtils.parseColonSeparatedArgs(reposStr)
			.stream()
			.map(p -> new Pair<>(
				new RepoId(UUID.fromString(p.getFirst())),
				p.getSecond().stream().map(BranchName::fromName).collect(Collectors.toSet())
			))
			.collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

		RepoComparisonGraph result = comparison.generateGraph(dimension, repos, startTime, endTime);

		JsonDimension jsonDimension = new JsonDimension(
			result.getDimensionInfo().getDimension().getBenchmark(),
			result.getDimensionInfo().getDimension().getMetric(),
			result.getDimensionInfo().getUnit().getName(),
			result.getDimensionInfo().getInterpretation()
		);

		final List<JsonRepoCompareGraphData> repoResults = result.getData().stream()
			.map(data -> new JsonRepoCompareGraphData(
				data.getRepoId(),
				data.getEntries().stream().map(graphEntry -> new JsonRepoCompareGraphEntry(
					graphEntry.getHash().getHash(),
					graphEntry.getAuthor(),
					graphEntry.getAuthorDate().getEpochSecond(),
					graphEntry.getSummary(),
					graphEntry.getValue()
				)).collect(toList())
			)).collect(toList());

		return new GetReply(jsonDimension, repoResults);
	}

	private static class GetReply {

		private final JsonDimension dimension;
		private final List<JsonRepoCompareGraphData> repos;

		public GetReply(JsonDimension dimension,
			List<JsonRepoCompareGraphData> repos) {
			this.dimension = dimension;
			this.repos = repos;
		}
	}

}
