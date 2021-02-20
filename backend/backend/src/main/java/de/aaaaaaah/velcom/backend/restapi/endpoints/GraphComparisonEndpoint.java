package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.FullCommit;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.restapi.endpoints.utils.EndpointUtils;
import de.aaaaaaah.velcom.backend.restapi.exception.InvalidQueryParamsException;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import de.aaaaaaah.velcom.shared.util.Either;
import de.aaaaaaah.velcom.shared.util.Pair;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final DimensionReadAccess dimensionAccess;

	public GraphComparisonEndpoint(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		DimensionReadAccess dimensionAccess) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.dimensionAccess = dimensionAccess;
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

		// Retrieve the graph information.
		//
		// You are standing in front of a dense forest. The forest is full of streams. If you venture
		// any further, you might slip. Do you want to continue? [Y/n]
		List<JsonGraphRepo> repos = EndpointUtils.parseRepos(reposStr)
			.entrySet()
			.stream()
			.map(entry -> {
				// Iterating over all repos (and associated branches) from the request...
				RepoId repoId = entry.getKey();
				Set<BranchName> branches = entry.getValue();

				// Find the commits that will later be displayed in the graph
				List<Commit> commits = commitAccess.getCommitsBetween(repoId, branches, startTime, endTime);
				List<FullCommit> fullCommits = commitAccess.promoteCommits(commits);
				Set<CommitHash> commitHashes = commits.stream().map(Commit::getHash).collect(toSet());

				// Find the latest run belonging to each commit
				Set<RunId> latestRunIds = new HashSet<>(
					benchmarkAccess.getLatestRunIds(repoId, commitHashes).values());
				Map<CommitHash, Run> runs = benchmarkAccess.getRuns(latestRunIds).stream()
					.collect(toMap(
						run -> run.getSource().getLeft().map(CommitSource::getHash).orElse(null),
						run -> run
					));

				// Collect the commit and run information as JsonGraphCommits
				List<JsonGraphCommit> graphCommits = fullCommits.stream()
					.map(commit -> new JsonGraphCommit(
						commit.getHashAsString(),
						commit.getParentHashes().stream().map(CommitHash::getHash).collect(toList()),
						commit.getAuthor(),
						commit.getCommitterDate().getEpochSecond(),
						commit.getSummary(),
						// Retrieve the value corresponding to our dimension
						Optional.ofNullable(runs.get(commit.getHash()))
							.map(Run::getResult)
							.flatMap(Either::getRight)
							.flatMap(measurements -> measurements.stream()
								.filter(measurement -> measurement.getDimension().equals(dimension))
								.findFirst())
							.map(Measurement::getContent)
							.flatMap(Either::getRight)
							.map(MeasurementValues::getAverageValue)
							.orElse(null)
					))
					.collect(toList());

				// We've now collected all information required for this particular repository
				return new JsonGraphRepo(repoId.getId(), graphCommits);
			})
			.collect(toList());

		DimensionInfo dimensionInfo = dimensionAccess.getDimensionInfo(dimension);
		JsonDimension jsonDimension = JsonDimension.fromDimensionInfo(dimensionInfo);

		return new GetReply(jsonDimension, repos);
	}

	private static class GetReply {

		public final JsonDimension dimension;
		public final List<JsonGraphRepo> repos;

		public GetReply(JsonDimension dimension, List<JsonGraphRepo> repos) {
			this.dimension = dimension;
			this.repos = repos;
		}
	}

	private static class JsonGraphRepo {

		public final UUID repoId;
		public final List<JsonGraphCommit> commits;

		public JsonGraphRepo(UUID repoId, List<JsonGraphCommit> commits) {
			this.repoId = repoId;
			this.commits = commits;
		}
	}

	private static class JsonGraphCommit {

		public final String hash;
		public final List<String> parents;
		public final String author;
		public final long committerDate;
		public final String summary;
		@Nullable
		public final Double value;

		public JsonGraphCommit(String hash, List<String> parents, String author, long committerDate,
			String summary, @Nullable Double value) {

			this.hash = hash;
			this.parents = parents;
			this.author = author;
			this.committerDate = committerDate;
			this.summary = summary;
			this.value = value;
		}
	}
}
