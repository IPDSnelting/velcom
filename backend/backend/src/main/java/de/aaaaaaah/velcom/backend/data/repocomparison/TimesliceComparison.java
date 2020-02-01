package de.aaaaaaah.velcom.backend.data.repocomparison;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.benchmark.BenchmarkAccess;
import de.aaaaaaah.velcom.backend.access.benchmark.CommitPerformance;
import de.aaaaaaah.velcom.backend.access.benchmark.Interpretation;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.benchmark.Unit;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.repo.BranchName;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.data.repocomparison.grouping.CommitGrouper;
import de.aaaaaaah.velcom.backend.data.repocomparison.grouping.GroupByDay;
import de.aaaaaaah.velcom.backend.data.repocomparison.grouping.GroupByHour;
import de.aaaaaaah.velcom.backend.data.repocomparison.grouping.GroupByWeek;
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
	private static final long HOURLY_THRESHOLD = 60 * 60 * 24 * 20; // 20 days
	// Difference of start and end time (in seconds) below which the daily grouper should be used.3
	private static final long DAILY_THRESHOLD = 60 * 60 * 24 * 7 * 20; // 20 weeks
	// If the start and end time difference is greater than this, the weekly grouper is used.

	private static final CommitGrouper<Long> HOURLY_GROUPER = new GroupByHour();
	private static final CommitGrouper<Long> DAILY_GROUPER = new GroupByDay();
	private static final CommitGrouper<Long> WEEKLY_GROUPER = new GroupByWeek();

	private final CommitAccess commitAccess;
	private final BenchmarkAccess benchmarkAccess;

	/**
	 * Constructs a new time slice comparison.
	 *
	 * @param commitAccess the commit access used to collect commit data
	 * @param benchmarkAccess the benchmark access used to collect benchmark data
	 */
	public TimesliceComparison(CommitAccess commitAccess, BenchmarkAccess benchmarkAccess) {
		this.commitAccess = commitAccess;
		this.benchmarkAccess = benchmarkAccess;
	}

	@Override
	public ComparisonGraph generateGraph(MeasurementName measurement,
		Map<RepoId, List<BranchName>> repoBranches,
		@Nullable Instant startTime, @Nullable Instant stopTime) {

		List<RepoGraphData> dataList = new ArrayList<>();

		repoBranches.forEach((repoId, branches) -> {
			collectData(repoId, measurement, branches, startTime, stopTime)
				.ifPresent(dataList::add);
		});

		return new ComparisonGraph(measurement, repoBranches, dataList);
	}

	private Optional<RepoGraphData> collectData(RepoId repoId, MeasurementName measurementName,
		Collection<BranchName> branches, @Nullable Instant startTime, @Nullable Instant stopTime) {

		Collection<String> branchNames = branches.stream()
			.map(BranchName::getName)
			.collect(toList());

		// 1.) Get commits
		final Collection<Commit> commits = commitAccess.getCommitsBetween(repoId, branchNames,
			startTime, stopTime);

		final Map<String, Commit> commitMap = new HashMap<>();
		commits.forEach(commit -> commitMap.put(commit.getHash().getHash(), commit));

		// 2.) Get relevant runs & commit performances
		final Collection<CommitPerformance> performances = benchmarkAccess.getCommitPerformances(
			repoId, measurementName, commits
		);

		if (performances.isEmpty()) {
			return Optional.empty(); // No graph data available
		}

		Instant oldestAuthorDate = null;
		CommitPerformance oldestPerformance = null;
		Instant youngestAuthorDate = null;
		CommitPerformance youngestPerformance = null;
		for (CommitPerformance performance : performances) {
			Commit commit = commitMap.get(performance.getCommitHash().getHash());
			Instant authorDate = commit.getAuthorDate();

			if (oldestAuthorDate == null || authorDate.isBefore(oldestAuthorDate)) {
				oldestAuthorDate = authorDate;
				oldestPerformance = performance;
			}

			if (youngestAuthorDate == null || authorDate.isAfter(youngestAuthorDate)) {
				youngestAuthorDate = authorDate;
				youngestPerformance = performance;
			}
		}

		Interpretation interpretation = youngestPerformance.getInterpretation();
		Unit unit = youngestPerformance.getUnit();

		if (startTime == null) {
			startTime = oldestAuthorDate;
		}
		if (stopTime == null) {
			stopTime = youngestAuthorDate;
		}

		// 3.) Build graph data (convert pairs to GraphEntry instances & group them)
		final Map<Long, List<GraphEntry>> groupMap = groupPerformancesToEntries(
			startTime, stopTime, performances, commitMap
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

	private Map<Long, List<GraphEntry>> groupPerformancesToEntries(Instant startTime,
		Instant stopTime, Collection<CommitPerformance> performances, Map<String,
		Commit> commitMap) {

		final CommitGrouper<Long> grouper = determineGrouper(startTime, stopTime);

		return performances.stream()
			.map(performance -> {
				Commit commit = commitMap.get(performance.getCommitHash().getHash());
				Objects.requireNonNull(commit, "commit null for: " + performance);

				return new GraphEntry(commit, performance.getAverage());
			})
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

}

