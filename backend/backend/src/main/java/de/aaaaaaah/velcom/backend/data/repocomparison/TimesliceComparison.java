package de.aaaaaaah.velcom.backend.data.repocomparison;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.caches.LatestRunCache;
import de.aaaaaaah.velcom.backend.access.caches.RunCache;
import de.aaaaaaah.velcom.backend.access.committaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.data.repocomparison.grouping.CommitGrouper;
import de.aaaaaaah.velcom.backend.data.repocomparison.grouping.GroupByDay;
import de.aaaaaaah.velcom.backend.data.repocomparison.grouping.GroupByHour;
import de.aaaaaaah.velcom.backend.data.repocomparison.grouping.GroupByWeek;
import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * A timeslice comparison compares the provided repositories with their respective commits in the
 * given time frame by splitting that time frame into smaller time slices and considering only the
 * best commit per repository for each time slice. This way the graph will be a lot smoother,
 * removing small one-off performance drops and peaks, which lends itself better for comparisons.
 */
public class TimesliceComparison implements RepoComparison {

	// Difference of start and end time (in seconds) below which the hourly grouper should be used.
	public static final long HOURLY_THRESHOLD = 60 * 60 * 24 * 20; // 20 days
	// Difference of start and end time (in seconds) below which the daily grouper should be used.3
	public static final long DAILY_THRESHOLD = 60 * 60 * 24 * 7 * 20; // 20 weeks
	// If the start and end time difference is greater than this, the weekly grouper is used.

	private static final CommitGrouper<Long> HOURLY_GROUPER = new GroupByHour();
	private static final CommitGrouper<Long> DAILY_GROUPER = new GroupByDay();
	private static final CommitGrouper<Long> WEEKLY_GROUPER = new GroupByWeek();

	private final BenchmarkReadAccess benchmarkAccess;
	private final CommitReadAccess commitAccess;
	private final DimensionReadAccess dimensionAccess;
	private final RunCache runCache;
	private final LatestRunCache latestRunCache;

	/**
	 * Constructs a new time slice comparison.
	 *
	 * @param benchmarkAccess the benchmark access used to collect benchmark data
	 * @param commitAccess the commit access used to collect commit data
	 * @param dimensionAccess the dimension access
	 * @param runCache the run cache
	 * @param latestRunCache the latest run cache
	 */
	public TimesliceComparison(BenchmarkReadAccess benchmarkAccess, CommitReadAccess commitAccess,
		DimensionReadAccess dimensionAccess, RunCache runCache, LatestRunCache latestRunCache) {

		this.benchmarkAccess = benchmarkAccess;
		this.commitAccess = commitAccess;
		this.dimensionAccess = dimensionAccess;
		this.runCache = runCache;
		this.latestRunCache = latestRunCache;
	}

	@Override
	@Timed(histogram = true)
	public RepoComparisonGraph generateGraph(Dimension dimension,
		Map<RepoId, Set<BranchName>> repoBranches, @Nullable Instant startTime,
		@Nullable Instant endTime) {

		final DimensionInfo dimensionInfo = dimensionAccess.getDimensionInfo(dimension);

		List<RepoGraphData> dataList = new ArrayList<>();
		Instant actualStartTime = startTime;
		Instant actualEndTime = endTime;

		for (RepoId repoId : repoBranches.keySet()) {
			Set<BranchName> branchNames = repoBranches.get(repoId);

			RepoDataResult result = collectData(repoId, dimensionInfo, branchNames, startTime, endTime)
				.orElse(null);

			if (result != null) {
				dataList.add(result.getGraphData());

				if (actualStartTime == null || result.getStartTime().isBefore(actualStartTime)) {
					actualStartTime = result.getStartTime();
				}

				if (actualEndTime == null || result.getEndTime().isAfter(actualEndTime)) {
					actualEndTime = result.getEndTime();
				}

			}
		}

		return new RepoComparisonGraph(dimensionInfo, dataList, actualStartTime, actualEndTime);
	}

	private Optional<RepoDataResult> collectData(RepoId repoId, DimensionInfo dimensionInfo,
		Set<BranchName> branchNames, @Nullable Instant startTime, @Nullable Instant endTime) {

		// 1.) Get commits
		Map<CommitHash, Commit> commitMap = commitAccess
			.getCommitsBetween(repoId, branchNames, startTime, endTime)
			.stream()
			.collect(toMap(Commit::getHash, commit -> commit));

		// 2.) Get relevant runs & values
		Map<CommitHash, Run> latestRuns = latestRunCache
			.getLatestRuns(benchmarkAccess, runCache, repoId, commitMap.keySet());

		if (latestRuns.isEmpty()) {
			return Optional.empty(); // No graph data available
		}

		// 3.) Filter out unsuccessful measurements
		Map<CommitHash, Measurement> measurementMap = new HashMap<>();

		latestRuns.forEach((hash, run) -> findMeasurement(dimensionInfo.getDimension(), run)
			.filter(m -> m.getContent().isRight())
			.ifPresent(values -> measurementMap.put(hash, values)));

		if (measurementMap.isEmpty()) {
			return Optional.empty(); // No graph data available
		}

		// 4.) Determine additional information

		Instant oldestAuthorDate = null;
		Instant youngestAuthorDate = null;

		for (CommitHash commitHash : measurementMap.keySet()) {
			Commit commit = commitMap.get(commitHash);
			Objects.requireNonNull(commit, "commit not found: " + commitHash);

			Instant authorDate = commit.getAuthorDate();

			if (oldestAuthorDate == null || authorDate.isBefore(oldestAuthorDate)) {
				oldestAuthorDate = authorDate;
			}

			if (youngestAuthorDate == null || authorDate.isAfter(youngestAuthorDate)) {
				youngestAuthorDate = authorDate;
			}
		}

		if (startTime == null) {
			startTime = oldestAuthorDate;
		}
		if (endTime == null) {
			endTime = youngestAuthorDate;
		}

		// 5.) Build initial graph data (convert pairs to GraphEntry instances)
		List<GraphEntry> entries = new ArrayList<>();

		measurementMap.forEach((hash, measurement) -> {
			Commit commit = commitMap.get(hash);
			Objects.requireNonNull(commit, "commit not found: " + hash);

			MeasurementValues values = measurement.getContent().getRight().orElseThrow();
			entries.add(new GraphEntry(commit, values.getAverageValue()));
		});

		// 6.) Group commits into time slices
		final Map<Long, List<GraphEntry>> groupMap = groupEntries(
			startTime, endTime, entries, commitMap
		);

		// 7.) Find best entry for each time slice
		Map<Long, GraphEntry> bestEntries = new HashMap<>();
		Interpretation interp = dimensionInfo.getInterpretation();

		groupMap.forEach((groupingValue, groupedEntries) -> findBestEntry(groupedEntries, interp)
			.ifPresent(bestEntry -> bestEntries.put(groupingValue, bestEntry))
		);

		final List<GraphEntry> graphEntries = bestEntries.keySet().stream()
			.sorted()
			.map(bestEntries::get)
			.collect(toList());

		return Optional.of(new RepoDataResult(
			new RepoGraphData(repoId, graphEntries),
			startTime,
			endTime
		));
	}

	private Map<Long, List<GraphEntry>> groupEntries(Instant startTime,
		Instant stopTime, Collection<GraphEntry> entries, Map<CommitHash, Commit> commitMap) {

		final CommitGrouper<Long> grouper = determineGrouper(startTime, stopTime);

		return entries.stream()
			.collect(Collectors.groupingBy(entry -> grouper.getGroup(
				entry.getAuthorDate().atZone(ZoneOffset.UTC)
			)));
	}

	private Optional<GraphEntry> findBestEntry(Collection<GraphEntry> entries,
		Interpretation interpretation) {

		// This assumes that the measurements all have the same interpretation as the most recently
		// benchmarked commit
		return entries.stream().reduce((a, b) -> {
			if (interpretation.equals(Interpretation.MORE_IS_BETTER)
				== (a.getValue() >= b.getValue())) {
				return a;
			} else {
				return b;
			}
		});
	}

	private CommitGrouper<Long> determineGrouper(@Nullable Instant startTime,
		@Nullable Instant stopTime) {

		CommitGrouper<Long> grouper;
		if (startTime != null && stopTime != null) {
			long difference = stopTime.getEpochSecond() - startTime.getEpochSecond();
			if (difference < HOURLY_THRESHOLD) {
				grouper = HOURLY_GROUPER;
			} else if (difference < DAILY_THRESHOLD) {
				grouper = DAILY_GROUPER;
			} else {
				grouper = WEEKLY_GROUPER;
			}
		} else {
			grouper = WEEKLY_GROUPER; // TODO choose grouper based on returned commits?
		}

		return grouper;
	}

	private Optional<Measurement> findMeasurement(Dimension name, Run run) {
		if (run.getResult().isRight()) {
			Collection<Measurement> measurements = run.getResult().getRight().get();

			Measurement measurement = measurements.stream()
				.filter(m -> m.getDimension().equals(name))
				.findAny()
				.orElse(null);

			return Optional.ofNullable(measurement);
		}

		return Optional.empty();
	}

}
