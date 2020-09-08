package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.RepoReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Branch;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.entities.Repo;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.restapi.endpoints.utils.EndpointUtils;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import de.aaaaaaah.velcom.backend.util.Either;
import de.aaaaaaah.velcom.backend.util.Pair;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for getting the detail graph.
 */
@Path("/graph/detail/{repoid}")
@Produces(MediaType.APPLICATION_JSON)
public class GraphDetailEndpoint {

	private final CommitReadAccess commitAccess;
	private final BenchmarkReadAccess benchmarkAccess;
	private final RepoReadAccess repoAccess;

	public GraphDetailEndpoint(CommitReadAccess commitAccess, BenchmarkReadAccess benchmarkAccess,
		RepoReadAccess repoAccess) {

		this.commitAccess = commitAccess;
		this.benchmarkAccess = benchmarkAccess;
		this.repoAccess = repoAccess;
	}

	@GET
	public GetReply get(
		@PathParam("repoid") UUID repoUuid,
		@QueryParam("start_time") @Nullable Long startTimeEpoch,
		@QueryParam("end_time") @Nullable Long endTimeEpoch,
		@QueryParam("duration") @Nullable Integer durationInSeconds,
		@QueryParam("dimensions") @NotNull String dimensionStr
	) {
		// Figure out tracked branches
		RepoId repoId = new RepoId(repoUuid);
		// By getting the tracked branches indirectly via the repo instead of directly from the
		// repoAccess, we ensure a NoSuchRepoException is thrown when the repo doesn't exist. This
		// should probably be handled differently though (e. g. by making repoAccess#getTrackedBranches
		// throw a NoSuchRepoException when no such repo exists).
		Repo repo = repoAccess.getRepo(repoId);
		Collection<BranchName> trackedBranches = repo.getTrackedBranches().stream()
			.map(Branch::getName)
			.collect(Collectors.toList());

		// Figure out which dimensions there actually are, and in which order we'll return them
		Set<Dimension> potentialDimensions = EndpointUtils.parseDimensions(dimensionStr);
		List<DimensionInfo> existingDimensions = new ArrayList<>(
			benchmarkAccess.getDimensionInfos(potentialDimensions).values());
		existingDimensions.sort(Comparator.comparing(DimensionInfo::getDimension));

		// Figure out the start and end time
		Pair<Instant, Instant> startAndEndTime = EndpointUtils
			.getStartAndEndTime(startTimeEpoch, endTimeEpoch, durationInSeconds);
		Instant startTime = startAndEndTime.getFirst();
		Instant endTime = startAndEndTime.getSecond();
		if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
			// TODO: 07.09.20 Ensure the http status code is correct
			throw new IllegalStateException();
		}

		// Figure out which commits we'll need to show
		List<Commit> commits = new ArrayList<>(
			commitAccess.getCommitsBetween(repoId, trackedBranches, startTime, endTime).values());
		commits.sort(Comparator.comparing(Commit::getAuthorDate));
		Set<CommitHash> hashes = commits.stream().map(Commit::getHash).collect(Collectors.toSet());

		// Obtain the relevant runs
		Map<CommitHash, Run> runs = benchmarkAccess.getLatestRuns(repoId, hashes);

		// Finally, put everything together.
		List<JsonDimension> jsonDimensions = existingDimensions.stream()
			.map(JsonDimension::fromDimensionInfo)
			.collect(Collectors.toList());
		List<JsonGraphCommit> jsonGraphCommits = commits.stream()
			.map(commit -> new JsonGraphCommit(
				commit.getHash().getHash(),
				commit.getParentHashes().stream().map(CommitHash::getHash).collect(Collectors.toList()),
				commit.getAuthor(),
				commit.getAuthorDate().getEpochSecond(),
				commit.getSummary(),
				extractValuesFromCommit(existingDimensions, runs, commit)
			))
			.collect(Collectors.toList());
		return new GetReply(jsonDimensions, jsonGraphCommits);
	}

	private static List<Double> extractValuesFromCommit(List<DimensionInfo> dimensions,
		Map<CommitHash, Run> runs, Commit commit) {

		// This would be way more readable if I could use do notation, I promise! :P

		return Optional.ofNullable(runs.get(commit.getHash()))
			.map(Run::getResult)
			.flatMap(Either::getRight)

			// Convert measurements into a map by dimension
			.map(values -> values.stream()
				.collect(Collectors.toMap(Measurement::getDimension, v -> v)))

			// Get the value for each dimension in order of dimension
			.map(values -> dimensions.stream()
				.map(dim -> Optional.ofNullable(values.get(dim.getDimension()))
					.map(Measurement::getContent)
					.flatMap(Either::getRight)
					.map(MeasurementValues::getAverageValue)
					.orElse(null))
				.collect(Collectors.toList()))

			// Create list of only nulls if there are no measurements
			.orElseGet(() -> dimensions.stream()
				.map(dim -> (Double) null)
				.collect(Collectors.toList()));
	}

	private static class GetReply {

		private final List<JsonDimension> dimensions;
		private final List<JsonGraphCommit> commits;

		public GetReply(List<JsonDimension> dimensions, List<JsonGraphCommit> commits) {
			this.dimensions = dimensions;
			this.commits = commits;
		}

		public List<JsonDimension> getDimensions() {
			return dimensions;
		}

		public List<JsonGraphCommit> getCommits() {
			return commits;
		}
	}

	private static class JsonGraphCommit {

		private final String hash;
		private final List<String> parents;
		private final String author;
		private final long authorDate;
		private final String summary;
		private final List<Double> values;

		public JsonGraphCommit(String hash, List<String> parents, String author, long authorDate,
			String summary, List<Double> values) {

			this.hash = hash;
			this.parents = parents;
			this.author = author;
			this.authorDate = authorDate;
			this.summary = summary;
			this.values = values;
		}

		public String getHash() {
			return hash;
		}

		public List<String> getParents() {
			return parents;
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

		public List<Double> getValues() {
			return values;
		}
	}
}
