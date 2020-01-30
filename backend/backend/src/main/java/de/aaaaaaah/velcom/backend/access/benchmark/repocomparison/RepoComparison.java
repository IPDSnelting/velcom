package de.aaaaaaah.velcom.backend.access.benchmark.repocomparison;

import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.codegen.db.tables.RunMeasurement.RUN_MEASUREMENT;
import static org.jooq.codegen.db.tables.RunMeasurementValue.RUN_MEASUREMENT_VALUE;
import static org.jooq.impl.DSL.max;

import de.aaaaaaah.velcom.backend.access.benchmark.Interpretation;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.benchmark.Unit;
import de.aaaaaaah.velcom.backend.access.benchmark.repocomparison.timeslice.CommitGrouper;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonGraphEntry;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonGraphRepoInfo;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.util.Pair;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.Record2;

public class RepoComparison {

	private final DatabaseStorage databaseStorage;

	public RepoComparison(DatabaseStorage databaseStorage) {
		this.databaseStorage = databaseStorage;
	}

	public JsonGraphRepoInfo getRepoInfo(RepoId repoId, Collection<Commit> commits,
		MeasurementName measurementName, CommitGrouper<Long> grouper) {

		// Step 1: Find out which runs we're interested in
		Collection<String> runIds = getRunIds(repoId, commits);

		// Step 2: Get all entries (commit + measurement values) that are relevant here.
		Collection<GraphEntry> tmpEntries =
			collectTmpEntries(repoId, commits, runIds, measurementName).stream()
				.filter(GraphEntry::hasValue)
				.collect(Collectors.toUnmodifiableList());

		// Step 3: Figure out which interpretation and unit to use
		Pair<Interpretation, Unit> interpretationAndUnit =
			getInterpretationAndUnit(repoId, runIds, measurementName);
		Interpretation interpretation = interpretationAndUnit.getFirst();
		Unit unit = interpretationAndUnit.getSecond();

		// Step 4: Group the entries based on their commits' author dates using a CommitGrouper
		Map<Long, List<GraphEntry>> groupedTmpEntries = groupTmpEntries(tmpEntries, grouper);

		// Step 5: Find the best entries for each segment
		Map<Long, GraphEntry> bestTmpEntries = new HashMap<>();
		groupedTmpEntries.forEach((groupingValue, groupedEntries) ->
			getBestTmpEntry(groupedEntries, interpretation).ifPresent(bestEntry ->
				bestTmpEntries.put(groupingValue, bestEntry)));

		// Step 6: Convert the TmpEntries into JsonGraphEntries in the correct order
		final List<JsonGraphEntry> orderedEntries = bestTmpEntries.keySet().stream()
			.sorted()
			.map(bestTmpEntries::get)
			.map(JsonGraphEntry::new)
			.collect(Collectors.toUnmodifiableList());

		// Step 7: Collect results in a JsonGraphRepoInfo
		return new JsonGraphRepoInfo(repoId, orderedEntries, interpretation, unit);
	}

	private Collection<String> getRunIds(RepoId repoId, Collection<Commit> commits) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			Collection<String> commitHashes = commits.stream()
				.map(Commit::getHash)
				.map(CommitHash::getHash)
				.collect(Collectors.toUnmodifiableSet());

			return db.select(RUN.ID, max(RUN.START_TIME))
				.from(RUN)
				.where(RUN.REPO_ID.eq(repoId.getId().toString()))
				.and(RUN.COMMIT_HASH.in(commitHashes))
				.groupBy(RUN.ID)
				.stream()
				.map(Record2::value1)
				.collect(Collectors.toUnmodifiableSet());
		}
	}

	private Collection<GraphEntry> collectTmpEntries(RepoId repoId, Collection<Commit> commits,
		Collection<String> runIds, MeasurementName measurementName) {

		// Map of commitHash -> GraphEntry
		// Initialize with commits
		Map<String, GraphEntry> tmpEntries = new HashMap<>();
		commits.forEach(commit -> {
			final String commitHash = commit.getHash().getHash();
			tmpEntries.put(commitHash, new GraphEntry(commit));
		});

		try (DSLContext db = databaseStorage.acquireContext()) {
			// Then collect all the measurements' values
			db.select(RUN.COMMIT_HASH, RUN_MEASUREMENT_VALUE.VALUE)
				.from(RUN
					.join(RUN_MEASUREMENT)
					.on(RUN.ID.eq(RUN_MEASUREMENT.RUN_ID))
					.join(RUN_MEASUREMENT_VALUE)
					.on(RUN_MEASUREMENT.ID.eq(RUN_MEASUREMENT_VALUE.MEASUREMENT_ID))
				)
				.where(RUN.REPO_ID.eq(repoId.getId().toString()))
				.and(RUN.ID.in(runIds))
				.and(RUN_MEASUREMENT.BENCHMARK.eq(measurementName.getBenchmark()))
				.and(RUN_MEASUREMENT.METRIC.eq(measurementName.getMetric()))
				.and(RUN_MEASUREMENT.ERROR_MESSAGE.isNull())
				.forEach(r -> {
					String commitHash = r.value1();
					double value = r.value2();

					tmpEntries.get(commitHash).addValue(value);
				});
		}

		return tmpEntries.values();
	}

	private Pair<Interpretation, Unit> getInterpretationAndUnit(RepoId repoId,
		Collection<String> runIds, MeasurementName measurementName) {

		try (DSLContext db = databaseStorage.acquireContext()) {
			// find the latest interpretation and unit
			final Optional<Record2<String, String>> interpretationAndUnit = db.select(
				RUN_MEASUREMENT.INTERPRETATION, RUN_MEASUREMENT.UNIT)
				.from(RUN
					.join(RUN_MEASUREMENT).on(RUN.ID.eq(RUN_MEASUREMENT.RUN_ID))
				)
				.where(RUN.REPO_ID.eq(repoId.getId().toString()))
				.and(RUN.ID.in(runIds))
				.and(RUN_MEASUREMENT.BENCHMARK.eq(measurementName.getBenchmark()))
				.and(RUN_MEASUREMENT.METRIC.eq(measurementName.getMetric()))
				.and(RUN_MEASUREMENT.ERROR_MESSAGE.isNull())
				.orderBy(RUN.START_TIME.desc())
				.limit(1)
				.fetchOptional();

			Interpretation interpretation = interpretationAndUnit
				.map(Record2::value1)
				.map(Interpretation::fromTextualRepresentation)
				.orElse(Interpretation.NEUTRAL);

			Unit unit = interpretationAndUnit
				.map(Record2::value2)
				.map(Unit::new)
				.orElse(Unit.EMPTY);

			return new Pair<>(interpretation, unit);
		}
	}

	private Map<Long, List<GraphEntry>> groupTmpEntries(Collection<GraphEntry> tmpEntries,
		CommitGrouper<Long> grouper) {

		return tmpEntries.stream()
			.collect(Collectors.groupingBy(entry -> grouper.getGroup(
				entry.getCommit().getAuthorDate().atZone(ZoneOffset.UTC)
			)));
	}

	private Optional<GraphEntry> getBestTmpEntry(Collection<GraphEntry> tmpEntries,
		Interpretation interpretation) {

		// This assumes that the measurements all have the same interpretation as the most recently
		// benchmarked commit
		return tmpEntries.stream().reduce((a, b) -> {
			if (interpretation.equals(Interpretation.MORE_IS_BETTER)
				== (a.getValue() >= b.getValue())) {
				return a;
			} else {
				return b;
			}
		});
	}
}
