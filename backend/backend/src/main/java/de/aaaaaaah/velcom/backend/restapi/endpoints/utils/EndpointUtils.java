package de.aaaaaaah.velcom.backend.restapi.endpoints.utils;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.access.caches.LatestRunCache;
import de.aaaaaaah.velcom.backend.access.caches.RunCache;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.FullCommit;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.data.significance.SignificanceFactors;
import de.aaaaaaah.velcom.backend.restapi.exception.InvalidQueryParamsException;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonResult;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import de.aaaaaaah.velcom.shared.util.Pair;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * A utility class for helper functions specific to the REST API endpoints.
 */
public class EndpointUtils {

	private EndpointUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Obtain a {@link Run} either by its ID or by a commit.
	 *
	 * @param benchmarkAccess a {@link BenchmarkReadAccess}
	 * @param runCache a {@link RunCache}
	 * @param latestRunCache a {@link LatestRunCache}
	 * @param id the run's ID, or the commit's repo id, if {@code hash} is specified
	 * @param hash the commit's hash, if the run should be obtained through a commit
	 * @return the run if it can be found
	 * @throws NoSuchRunException if the specified run does not exist
	 */
	public static Run getRun(BenchmarkReadAccess benchmarkAccess, RunCache runCache,
		LatestRunCache latestRunCache, UUID id, @Nullable String hash) throws NoSuchRunException {

		if (hash == null) {
			RunId runId = new RunId(id);
			return runCache.getRun(benchmarkAccess, runId);
		} else {
			RepoId repoId = new RepoId(id);
			CommitHash commitHash = new CommitHash(hash);
			return latestRunCache.getLatestRun(benchmarkAccess, runCache, repoId, commitHash)
				.orElseThrow(() -> new NoSuchRunException(repoId, commitHash));
		}
	}

	/**
	 * Create a {@link JsonRun} from a {@link Run} and a few other variables.
	 *
	 * @param dimensionAccess a {@link DimensionReadAccess}
	 * @param commitAccess a {@link CommitReadAccess}
	 * @param run the run
	 * @param significanceFactors the current significance factors (required for stddev
	 * 	calculations)
	 * @param allValues whether the full lists of values should also be included for each
	 * 	measurement
	 * @return the created {@link JsonRun}
	 */
	public static JsonRun fromRun(DimensionReadAccess dimensionAccess, CommitReadAccess commitAccess,
		Run run, SignificanceFactors significanceFactors, boolean allValues) {

		JsonSource source = JsonSource.fromSource(run.getSource(), commitAccess);
		Map<Dimension, DimensionInfo> dimensionInfos = dimensionAccess
			.getDimensionInfoMap(run.getAllDimensionsUsed());

		return new JsonRun(
			run.getId().getId(),
			run.getAuthor(),
			run.getRunnerName(),
			run.getRunnerInfo(),
			run.getStartTime().getEpochSecond(),
			run.getStopTime().getEpochSecond(),
			source,
			run.getResult().consume(
				JsonResult::fromRunError,
				it -> JsonResult.fromMeasurements(it, dimensionInfos, significanceFactors, allValues)
			)
		);
	}

	/**
	 * Parse a string of colon-separated arguments. The string is split into sections by double colons
	 * "::". The sections are split into elements by single colons ":". Each sections must contain at
	 * least two elements, the first of which is the section's name.
	 *
	 * @param args the string to parse
	 * @return the parsed sections and arguments
	 */
	public static List<Pair<String, List<String>>> parseColonSeparatedArgs(String args) {
		return Arrays.stream(args.split("::"))
			.map(s -> {
				String[] elements = s.split(":");
				if (elements.length == 0 || elements[0].equals("")) {
					throw new ArgumentParseException("there needs to be at least one section");
				} else if (elements.length < 2) {
					throw new ArgumentParseException("section \"" + s + "\" needs at least two elements");
				}

				String sectionName = elements[0];
				List<String> sectionElements = Arrays.stream(elements).skip(1).collect(Collectors.toList());
				return new Pair<>(sectionName, sectionElements);
			})
			.collect(Collectors.toList());
	}

	/**
	 * Parse a string of colon-separated arguments into dimensions. Each section represents multiple
	 * dimensions with the same benchmark. The section name is the benchmark and the section's other
	 * elements are the metrics. The same rules as with {@link #parseColonSeparatedArgs(String)}
	 * apply.
	 *
	 * @param args the string to parse
	 * @return the parsed dimensions
	 */
	public static Set<Dimension> parseDimensions(String args) {
		return parseColonSeparatedArgs(args).stream()
			.flatMap(pair -> pair.getSecond().stream()
				.map(elem -> new Dimension(pair.getFirst(), elem)))
			.collect(Collectors.toSet());
	}

	/**
	 * Parse a string of colon-separated arguments into repos and their branches. Each section
	 * represents a repo with the repo name being the section's name. Each further element in the
	 * section is the name of a branch in the corresponding repo. The he same rules as with {@link
	 * #parseColonSeparatedArgs(String)} apply.
	 *
	 * @param args the string to parse
	 * @return the parsed repos and branch names
	 */
	public static Map<RepoId, Set<BranchName>> parseRepos(String args) {
		return parseColonSeparatedArgs(args).stream()
			.collect(Collectors.toMap(
				pair -> new RepoId(UUID.fromString(pair.getFirst())),
				pair -> pair.getSecond().stream().map(BranchName::fromName).collect(Collectors.toSet())
			));
	}

	/**
	 * Get the start and end time of an interval that is defined by zero, one or two of three possible
	 * values. If the start time and duration parameters are both null, the returned start time is
	 * null as this function will not guess a start time. Otherwise, the returned start and end time
	 * will never be null.
	 *
	 * @param startTimeEpoch the interval's start time in epoch time (seconds)
	 * @param endTimeEpoch the interval's end time in epoch time (seconds)
	 * @param durationInSeconds the interval's duration in seconds
	 * @return the interval's start and end time
	 * @throws InvalidQueryParamsException if all three parameters are not null
	 */
	public static Pair<Instant, Instant> getStartAndEndTime(@Nullable Long startTimeEpoch,
		@Nullable Long endTimeEpoch, @Nullable Integer durationInSeconds) {

		// Parse startTime, endTime and duration
		Instant startTime = startTimeEpoch == null ? null : Instant.ofEpochSecond(startTimeEpoch);
		Instant endTime = endTimeEpoch == null ? null : Instant.ofEpochSecond(endTimeEpoch);
		Duration duration = durationInSeconds == null ? null : Duration.ofSeconds(durationInSeconds);

		// Relatively ugly start and end time logic, but I don't see any obvious way of making it more
		// elegant and concise.
		if (startTime != null && endTime != null && duration != null) {
			throw new InvalidQueryParamsException(
				"duration, start_time and end_time can't all be specified at the same time");
		} else if (duration != null) {
			if (startTime != null) {
				endTime = startTime.plus(duration);
			} else if (endTime != null) {
				startTime = endTime.minus(duration);
			} else {
				endTime = Instant.now();
				startTime = endTime.minus(duration);
			}
		} else {
			if (endTime == null) {
				endTime = Instant.now();
			}
		}

		return new Pair<>(startTime, endTime);
	}

	/**
	 * @param commits the commits to sort
	 * @param hashes a map from hash to commit <em>for <strong>exactly</strong> the commits in the
	 * 	commits list</em>
	 * @return a mutable list containing a topological ordering of the input commits
	 */
	public static List<FullCommit> topologicalSort(List<FullCommit> commits,
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
}
