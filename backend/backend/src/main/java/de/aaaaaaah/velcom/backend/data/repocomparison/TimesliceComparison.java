package de.aaaaaaah.velcom.backend.data.repocomparison;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.data.repocomparison.grouping.CommitGrouper;
import de.aaaaaaah.velcom.backend.data.repocomparison.grouping.GroupByDay;
import de.aaaaaaah.velcom.backend.data.repocomparison.grouping.GroupByHour;
import de.aaaaaaah.velcom.backend.data.repocomparison.grouping.GroupByWeek;
import de.aaaaaaah.velcom.backend.newaccess.BenchmarkReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.CommitReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.newaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.entities.Run;
import de.aaaaaaah.velcom.backend.newaccess.entities.Unit;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

	private final CommitReadAccess commitAccess;
	private final BenchmarkReadAccess benchmarkAccess;

	/**
	 * Constructs a new time slice comparison.
	 *
	 * @param commitAccess the commit access used to collect commit data
	 * @param benchmarkAccess the benchmark access used to collect benchmark data
	 */
	public TimesliceComparison(CommitReadAccess commitAccess, BenchmarkReadAccess benchmarkAccess) {
		this.commitAccess = commitAccess;
		this.benchmarkAccess = benchmarkAccess;
	}

	@Override
	public ComparisonGraph generateGraph(MeasurementName measurement,
		Map<RepoId, List<BranchName>> repoBranches,
		@Nullable Instant startTime, @Nullable Instant stopTime) {

		List<RepoGraphData> dataList = new ArrayList<>();

		repoBranches.forEach(
			(repoId, branches) ->
				collectData(repoId, measurement, branches, startTime, stopTime)
					.ifPresent(dataList::add)
		);

		return new ComparisonGraph(measurement, repoBranches, dataList);
	}

	private Optional<RepoGraphData> collectData(RepoId repoId, MeasurementName measurementName,
		Collection<BranchName> branches, @Nullable Instant startTime, @Nullable Instant stopTime) {

		// 1.) Get commits
		Map<CommitHash, Commit> commitMap = commitAccess.getCommitsBetween(repoId,
			branches, startTime, stopTime);

		// 2.) Get relevant runs & values
		Map<CommitHash, Run> latestRuns = benchmarkAccess.getLatestRuns(repoId, commitMap.keySet());

		if (latestRuns.isEmpty()) {
			return Optional.empty(); // No graph data available
		}

		Map<CommitHash, MeasurementValues> valueMap = new HashMap<>();

		latestRuns.forEach((hash, run) -> {
			findMeasurement(measurementName, run).ifPresent(values -> valueMap.put(hash, values));
		});

		if (valueMap.isEmpty()) {
			return Optional.empty(); // No graph data available
		}

		Instant oldestAuthorDate = null;
		Instant youngestAuthorDate = null;
		MeasurementValues youngestValues = null;

		for (CommitHash commitHash : valueMap.keySet()) {
			Commit commit = commitMap.get(commitHash);
			Objects.requireNonNull(commit, "commit not found: " + commitHash);

			Instant authorDate = commit.getAuthorDate();

			if (oldestAuthorDate == null || authorDate.isBefore(oldestAuthorDate)) {
				oldestAuthorDate = authorDate;
			}

			if (youngestAuthorDate == null || authorDate.isAfter(youngestAuthorDate)) {
				youngestAuthorDate = authorDate;
				youngestValues = valueMap.get(commitHash);
			}
		}

		Interpretation interpretation = youngestValues.getInterpretation();
		Unit unit = youngestValues.getUnit();

		if (startTime == null) {
			startTime = oldestAuthorDate;
		}
		if (stopTime == null) {
			stopTime = youngestAuthorDate;
		}

		// 3.) Build graph data (convert pairs to GraphEntry instances & group them)
		List<GraphEntry> entries = new ArrayList<>();

		valueMap.forEach((hash, values) -> {
			Commit commit = commitMap.get(hash);
			Objects.requireNonNull(commit, "commit not found: " + hash);

			entries.add(new GraphEntry(commit, values.getAverageValue()));
		});

		final Map<Long, List<GraphEntry>> groupMap = groupEntries(
			startTime, stopTime, entries, commitMap
		);

		// 4.) Find best entries for each time slice
		Map<Long, GraphEntry> bestEntries = new HashMap<>();
		groupMap.forEach(
			(groupingValue, groupedEntries) -> findBestEntry(groupedEntries, interpretation)
				.ifPresent(bestEntry -> bestEntries.put(groupingValue, bestEntry))
		);

		final List<GraphEntry> graphEntries = bestEntries.keySet().stream()
			.sorted()
			.map(bestEntries::get)
			.collect(toList());

		return Optional.of(new RepoGraphData(repoId, graphEntries, interpretation, unit));
	}

	private Map<Long, List<GraphEntry>> groupEntries(Instant startTime,
		Instant stopTime, Collection<GraphEntry> entries, Map<CommitHash, Commit> commitMap) {

		final CommitGrouper<Long> grouper = determineGrouper(startTime, stopTime);

		return entries.stream()
			.collect(Collectors.groupingBy(entry -> grouper.getGroup(
				entry.getCommit().getAuthorDate().atZone(ZoneOffset.UTC)
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

	private Optional<MeasurementValues> findMeasurement(MeasurementName name, Run run) {
		if (run.getMeasurements().isPresent()) {
			Collection<Measurement> measurements = run.getMeasurements().get();

			Measurement measurement = measurements.stream()
				.filter(m -> m.getMeasurementName().equals(name))
				.findAny()
				.orElse(null);

			if (measurement != null && measurement.getContent().getRight().isPresent()) {
				MeasurementValues values = measurement.getContent().getRight().get();
				return Optional.of(values);
			}
		}

		return Optional.empty();
	}

}

