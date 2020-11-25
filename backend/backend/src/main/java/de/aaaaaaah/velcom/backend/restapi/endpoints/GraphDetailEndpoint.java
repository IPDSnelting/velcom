package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.FullCommit;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.exceptions.NoSuchRepoException;
import de.aaaaaaah.velcom.backend.restapi.endpoints.utils.EndpointUtils;
import de.aaaaaaah.velcom.backend.restapi.exception.InvalidQueryParamsException;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import de.aaaaaaah.velcom.shared.util.Pair;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
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

	private static final String NO_RUN_FOUND = "N";
	private static final String NO_MEASUREMENT_FOUND = "O";
	private static final String RUN_FAILED = "R";
	private static final String MEASUREMENT_FAILED = "M";

	private final CommitReadAccess commitAccess;
	private final BenchmarkReadAccess benchmarkAccess;
	private final DimensionReadAccess dimensionAccess;
	private final RepoReadAccess repoAccess;

	public GraphDetailEndpoint(CommitReadAccess commitAccess, BenchmarkReadAccess benchmarkAccess,
		DimensionReadAccess dimensionAccess, RepoReadAccess repoAccess) {

		this.commitAccess = commitAccess;
		this.benchmarkAccess = benchmarkAccess;
		this.dimensionAccess = dimensionAccess;
		this.repoAccess = repoAccess;
	}

	@GET
	@Timed(histogram = true)
	public GetReply get(
		@PathParam("repoid") UUID repoUuid,
		@QueryParam("start_time") @Nullable Long startTimeEpoch,
		@QueryParam("end_time") @Nullable Long endTimeEpoch,
		@QueryParam("duration") @Nullable Integer durationInSeconds,
		@QueryParam("dimensions") @NotNull String dimensionStr
	) throws NoSuchRepoException {
		// Figure out tracked branches
		RepoId repoId = new RepoId(repoUuid);
		repoAccess.guardRepoExists(repoId);

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
			throw new InvalidQueryParamsException("start time must be earlier than end time");
		}

		// Figure out which commits we'll need to show
		List<FullCommit> commits = new ArrayList<>(commitAccess.promoteCommits(
			commitAccess.getTrackedCommitsBetween(repoId, startTime, endTime)
		));

		Map<CommitHash, FullCommit> hashes = commits.stream()
			.collect(toMap(
				Commit::getHash,
				it -> it
			));

		commits = topologicalSort(commits, hashes);
		commits.sort(Comparator.comparing(Commit::getCommitterDate));

		// Obtain the relevant runs
		Map<CommitHash, Run> runs = benchmarkAccess.getLatestRuns(repoId, hashes.keySet());

		// Finally, put everything together.
		List<JsonDimension> jsonDimensions = existingDimensions.stream()
			.map(JsonDimension::fromDimensionInfo)
			.collect(Collectors.toList());
		List<JsonGraphCommit> jsonGraphCommits = commits.stream()
			.map(commit -> new JsonGraphCommit(
				commit.getHash().getHash(),
				commit.getParentHashes().stream().map(CommitHash::getHash).collect(Collectors.toList()),
				commit.getAuthor(),
				commit.getCommitterDate().getEpochSecond(),
				commit.getSummary(),
				extractValuesFromCommit(existingDimensions, runs, commit)
			))
			.collect(Collectors.toList());

		return new GetReply(jsonDimensions, jsonGraphCommits);
	}

	/**
	 * @param commits the commits to sort
	 * @param hashes a map from hash to commit <em>for <strong>exactly</strong> the commits in the
	 * 	commits list</em>
	 * @return a mutable list containing a topological ordering of the input commits
	 */
	private List<FullCommit> topologicalSort(List<FullCommit> commits,
		Map<CommitHash, FullCommit> hashes) {

		// Based on Khan's Algorithm
		// https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm

		List<FullCommit> topologicallySorted = new ArrayList<>();

		Queue<FullCommit> leaves = commits.stream()
			// Only consider commits in our commits list. Throw away all children *not* in the list, as
			// we are focusing on a small part of the graph and trying to sort only this part
			// topologically.
			// If we do not throw them away, we will find children outside the range, leading to too few
			// found leaf nodes
			.filter(it -> it.getChildHashes().stream().noneMatch(hashes::containsKey))
			.collect(toCollection(ArrayDeque::new));

		// We can not modify the actual children or remove edges. This is one of the pre-requisites for
		// Khan's Algorithm though, as it deletes all explored edges starting with the leaves.
		// Furthermore, not all children should be considered - only those that we know are in the
		// commits list. All other children are outside the time range and irrelevant, as they should
		// not appear in the graph.
		// If we do not exclude those here, we will have too many children and won't always end up with
		// *zero* leftover children, causing the Commit to not be recognized as a new leaf.
		Map<FullCommit, Set<CommitHash>> parentChildMap = commits.stream()
			.collect(toMap(
				it -> it,
				it -> it.getChildHashes().stream()
					.filter(hashes::containsKey)
					.collect(toCollection(HashSet::new))
			));

		while (!leaves.isEmpty()) {
			FullCommit commit = leaves.poll();
			topologicallySorted.add(commit);

			for (CommitHash parentHash : commit.getParentHashes()) {
				FullCommit parentCommit = hashes.get(parentHash);

				// outside of our time bound (i.e. commits i.e. hashes)
				if (parentCommit == null) {
					continue;
				}

				Set<CommitHash> existingchildren = parentChildMap.get(parentCommit);
				existingchildren.remove(commit.getHash());

				// This commit has no other children in our time window -> it is now a leaf!
				if (existingchildren.isEmpty()) {
					leaves.add(parentCommit);
				}
			}
		}

		// We appended to the end, so we now need to reverse it. Our leaves are the first entries
		// currently, but they should be the last.
		Collections.reverse(topologicallySorted);

		return topologicallySorted;
	}

	private static List<Object> extractValuesFromCommit(List<DimensionInfo> dimensions,
		Map<CommitHash, Run> runs, Commit commit) {

		Run run = runs.get(commit.getHash());
		if (run == null) {
			return createListFullOf(dimensions, NO_RUN_FOUND);
		}

		Optional<Collection<Measurement>> optionalMeasurements = run.getResult().getRight();
		if (optionalMeasurements.isEmpty()) {
			return createListFullOf(dimensions, RUN_FAILED);
		}

		Map<Dimension, Measurement> measurements = optionalMeasurements.get().stream()
			.collect(toMap(Measurement::getDimension, m -> m));

		return dimensions.stream()
			.map(dim -> {
				Measurement measurement = measurements.get(dim.getDimension());
				if (measurement == null) {
					return NO_MEASUREMENT_FOUND;
				}

				Optional<MeasurementValues> values = measurement.getContent().getRight();
				if (values.isEmpty()) {
					return MEASUREMENT_FAILED;
				}

				return values.get().getAverageValue();
			})
			.collect(Collectors.toList());
	}

	private static List<Object> createListFullOf(List<DimensionInfo> dimensions, String string) {
		return dimensions.stream()
			.map(dim -> string)
			.collect(Collectors.toList());
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
		private final long committerDate;
		private final String summary;
		// A list of Doubles and Strings
		private final List<Object> values;

		public JsonGraphCommit(String hash, List<String> parents, String author, long comitterDate,
			String summary, List<Object> values) {

			this.hash = hash;
			this.parents = parents;
			this.author = author;
			this.committerDate = comitterDate;
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

		public long getCommitterDate() {
			return committerDate;
		}

		public String getSummary() {
			return summary;
		}

		public List<Object> getValues() {
			return values;
		}
	}
}
