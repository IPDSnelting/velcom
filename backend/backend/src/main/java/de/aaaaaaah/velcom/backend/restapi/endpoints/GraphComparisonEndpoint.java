package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.data.repocomparison.RepoComparison;
import de.aaaaaaah.velcom.backend.data.repocomparison.RepoComparisonGraph;
import de.aaaaaaah.velcom.backend.restapi.endpoints.utils.EndpointUtils;
import de.aaaaaaah.velcom.backend.restapi.exception.InvalidQueryParamsException;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import de.aaaaaaah.velcom.shared.util.Pair;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for getting the repo comparison graph.
 */
@Path("/graph/comparison")
@Produces(MediaType.APPLICATION_JSON)
public class GraphComparisonEndpoint {

	private final DimensionReadAccess dimensionAccess;
	private final RepoComparison comparison;

	public GraphComparisonEndpoint(DimensionReadAccess dimensionAccess, RepoComparison comparison) {
		this.dimensionAccess = dimensionAccess;
		this.comparison = comparison;
	}

	@GET
	@Timed(histogram = true)
	public GetReply get(
		@QueryParam("repos") @NotNull String reposStr,
		@QueryParam("start_time") @Nullable Long startTimeEpoch,
		@QueryParam("end_time") @Nullable Long endTimeEpoch,
		@QueryParam("duration") @Nullable Integer durationInSeconds,
		@QueryParam("dimension") @NotNull String dimensionStr
	) {
		// Parse dimension
		Set<Dimension> dimensionSet = EndpointUtils.parseDimensions(dimensionStr);
		if (dimensionSet.size() != 1) {
			throw new InvalidQueryParamsException("exactly one dimension must be specified");
		}

		Dimension dimension = dimensionSet.iterator().next();
		dimensionAccess.guardDimensionExists(dimension);

		// Figure out the start and end time
		Pair<Instant, Instant> startAndEndTime = EndpointUtils
			.getStartAndEndTime(startTimeEpoch, endTimeEpoch, durationInSeconds);
		Instant startTime = startAndEndTime.getFirst();
		Instant endTime = startAndEndTime.getSecond();
		if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
			throw new InvalidQueryParamsException("start time must be earlier than end time");
		}

		final Map<RepoId, Set<BranchName>> repos = EndpointUtils.parseRepos(reposStr);
		RepoComparisonGraph result = comparison.generateGraph(dimension, repos, startTime, endTime);
		JsonDimension jsonDimension = JsonDimension.fromDimensionInfo(result.getDimensionInfo());

		final List<JsonGraphRepo> repoResults = result.getData().stream()
			.map(data -> new JsonGraphRepo(
				data.getRepoId().getId(),
				data.getEntries().stream().map(graphEntry -> new JsonGraphCommit(
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
		private final List<JsonGraphRepo> repos;

		public GetReply(JsonDimension dimension, List<JsonGraphRepo> repos) {
			this.dimension = dimension;
			this.repos = repos;
		}

		public JsonDimension getDimension() {
			return dimension;
		}

		public List<JsonGraphRepo> getRepos() {
			return repos;
		}
	}

	private static class JsonGraphRepo {

		private final UUID repoId;
		private final List<JsonGraphCommit> commits;

		public JsonGraphRepo(UUID repoId, List<JsonGraphCommit> commits) {
			this.repoId = repoId;
			this.commits = commits;
		}

		public UUID getRepoId() {
			return repoId;
		}

		public List<JsonGraphCommit> getCommits() {
			return commits;
		}
	}

	private static class JsonGraphCommit {

		private final String hash;
		private final String author;
		private final long authorDate;
		private final String summary;
		private final double value;

		public JsonGraphCommit(String hash, String author, long authorDate, String summary,
			double value) {

			this.hash = hash;
			this.author = author;
			this.authorDate = authorDate;
			this.summary = summary;
			this.value = value;
		}

		public String getHash() {
			return hash;
		}

		public String getAuthor() {
			return author;
		}

		public long getAuthorDate() {
			return authorDate;
		}

		public String getSummary() {
			return summary;
		}

		public double getValue() {
			return value;
		}
	}
}
