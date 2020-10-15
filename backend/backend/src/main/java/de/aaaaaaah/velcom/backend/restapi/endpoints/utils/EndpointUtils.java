package de.aaaaaaah.velcom.backend.restapi.endpoints.utils;

import de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.restapi.exception.InvalidQueryParamsException;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonResult;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonSource;
import de.aaaaaaah.velcom.shared.util.Pair;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class EndpointUtils {

	private EndpointUtils() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Obtain a {@link Run} either by its ID or by a commit.
	 *
	 * @param benchmarkAccess a {@link BenchmarkReadAccess}
	 * @param id the run's ID, or the commit's repo id, if {@code hash} is specified
	 * @param hash the commit's hash, if the run should be obtained through a commit
	 * @return the run if it can be found
	 */
	public static Run getRun(BenchmarkReadAccess benchmarkAccess, UUID id, @Nullable String hash)
		throws NoSuchRunException {

		if (hash == null) {
			RunId runId = new RunId(id);
			return benchmarkAccess.getRun(runId);
		} else {
			RepoId repoId = new RepoId(id);
			CommitHash commitHash = new CommitHash(hash);
			// TODO use exception instead
			return benchmarkAccess.getLatestRun(repoId, commitHash).get();
		}
	}

	/**
	 * Create a {@link JsonRun} from a {@link Run} and a few other variables.
	 *
	 * @param benchmarkAccess a {@link BenchmarkReadAccess}
	 * @param commitAccess a {@link CommitReadAccess}
	 * @param run the run
	 * @param allValues whether the full lists of values should also be included for each
	 * 	measurement
	 * @return the created {@link JsonRun}
	 */
	public static JsonRun fromRun(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		Run run, boolean allValues) {

		JsonSource source = JsonSource.fromSource(run.getSource(), commitAccess);
		Map<Dimension, DimensionInfo> dimensionInfos = benchmarkAccess
			.getDimensionInfos(run.getAllDimensionsUsed());

		if (run.getResult().isLeft()) {
			return new JsonRun(
				run.getId().getId(),
				run.getAuthor(),
				run.getRunnerName(),
				run.getRunnerInfo(),
				run.getStartTime().getEpochSecond(),
				run.getStopTime().getEpochSecond(),
				source,
				JsonResult.fromRunError(run.getResult().getLeft().get())
			);
		} else {
			return new JsonRun(
				run.getId().getId(),
				run.getAuthor(),
				run.getRunnerName(),
				run.getRunnerInfo(),
				run.getStartTime().getEpochSecond(),
				run.getStopTime().getEpochSecond(),
				source,
				JsonResult.fromMeasurements(
					run.getResult().getRight().get(),
					dimensionInfos,
					allValues
				)
			);
		}
	}

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

	public static Set<Dimension> parseDimensions(String args) {
		return parseColonSeparatedArgs(args).stream()
			.flatMap(pair -> pair.getSecond().stream()
				.map(elem -> new Dimension(pair.getFirst(), elem)))
			.collect(Collectors.toSet());
	}

	public static Map<RepoId, Set<BranchName>> parseRepos(String args) {
		return parseColonSeparatedArgs(args).stream()
			.collect(Collectors.toMap(
				pair -> new RepoId(UUID.fromString(pair.getFirst())),
				pair -> pair.getSecond().stream().map(BranchName::fromName).collect(Collectors.toSet())
			));
	}

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
}
