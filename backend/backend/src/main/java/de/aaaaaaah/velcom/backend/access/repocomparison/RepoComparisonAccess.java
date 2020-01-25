package de.aaaaaaah.velcom.backend.access.repocomparison;

import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.codegen.db.tables.RunMeasurement.RUN_MEASUREMENT;
import static org.jooq.codegen.db.tables.RunMeasurementValue.RUN_MEASUREMENT_VALUE;

import de.aaaaaaah.velcom.backend.access.benchmark.Interpretation;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.benchmark.Unit;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.access.repocomparison.timeslice.CommitGrouper;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonRun;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.Record1;

public class RepoComparisonAccess {

	private final DatabaseStorage databaseStorage;
	private final CommitGrouper<Long> commitGrouper;

	public RepoComparisonAccess(DatabaseStorage databaseStorage,
		CommitGrouper<Long> commitGrouper) {

		this.databaseStorage = databaseStorage;
		this.commitGrouper = commitGrouper;
	}

	public List<JsonRun> getRelevantRuns(RepoId repoId, Collection<Commit> commits,
		MeasurementName measurementName) {

		// Step 1: Collect the commits and their measurements from the db
		final Collection<TmpCommit> tmpCommits =
			collectTmpCommits(repoId, commits, measurementName);

		// Step 2: Group the commits based on their author date using a CommitGrouper
		final Map<Long, List<TmpCommit>> groupedTmpCommits = groupTmpCommits(tmpCommits);

		// Step 3: Find the best commit for each group of commits
		// If only there was a mapMayeWithKey... :P
		final Map<Long, TmpCommit> bestTmpCommits = new HashMap<>();
		groupedTmpCommits.forEach((groupingValue, groupedCommits) ->
			getBestTmpCommit(groupedCommits, measurementName).ifPresent(bestCommit ->
				bestTmpCommits.put(groupingValue, bestCommit)));

		// Step 4: Convert the TmpCommits into JsonRuns and return them in the correct order
		return bestTmpCommits.keySet().stream()
			.sorted()
			.map(bestTmpCommits::get)
			.map(JsonRun::new)
			.collect(Collectors.toUnmodifiableList());
	}

	public Collection<TmpCommit> collectTmpCommits(RepoId repoId, Collection<Commit> commits,
		MeasurementName measurementName) {

		// Map of commitHash -> TmpCommit
		Map<String, TmpCommit> tmpCommits = new HashMap<>();
		commits.forEach(commit -> {
			final String commitHash = commit.getHash().getHash();
			tmpCommits.put(commitHash, new TmpCommit(commitHash, commit));
		});

		try (DSLContext db = databaseStorage.acquireContext()) {
			// The following two sql statements are more-or-less transcribed from
			// RepoComparison_sql_statements.sql in the resources directory.

			// First, find all valid run IDs
			final Set<String> runIds = commits.stream()
				.map(commit -> db.select(RUN.ID)
					.from(RUN)
					.where(RUN.REPO_ID.eq(repoId.getId().toString()))
					.and(RUN.COMMIT_HASH.eq(commit.getHash().getHash()))
					.orderBy(RUN.START_TIME.desc())
					.limit(1)
					.fetchOne())
				.filter(Objects::nonNull)
				.map(Record1::value1)
				.collect(Collectors.toUnmodifiableSet());

			// Then collect all the non-failed measurements
			db.select(RUN.COMMIT_HASH, RUN_MEASUREMENT.ID, RUN_MEASUREMENT.BENCHMARK,
				RUN_MEASUREMENT.METRIC,
				RUN_MEASUREMENT.INTERPRETATION, RUN_MEASUREMENT.UNIT)
				.from(RUN
					.join(RUN_MEASUREMENT).on(RUN.ID.eq(RUN_MEASUREMENT.RUN_ID))
				)
				.where(RUN.REPO_ID.eq(repoId.getId().toString()))
				.and(RUN.ID.in(runIds))
				.and(RUN_MEASUREMENT.ERROR_MESSAGE.isNull())
				.forEach(r -> {
					String commitHash = r.value1();
					MeasurementName name = new MeasurementName(r.value3(), r.value4());
					TmpMeasurement tmpMeasurement = new TmpMeasurement(
						r.value2(),
						measurementName, Interpretation.fromTextualRepresentation(r.value5()),
						new Unit(r.value6())
					);

					tmpCommits.get(commitHash).addMeasurement(name, tmpMeasurement);
				});

			// And then collect all those measurements' values
			db.select(RUN.COMMIT_HASH, RUN_MEASUREMENT.BENCHMARK, RUN_MEASUREMENT.METRIC,
				RUN_MEASUREMENT_VALUE.VALUE)
				.from(RUN
					.join(RUN_MEASUREMENT)
					.on(RUN.ID.eq(RUN_MEASUREMENT.RUN_ID))
					.join(RUN_MEASUREMENT_VALUE)
					.on(RUN_MEASUREMENT.ID.eq(RUN_MEASUREMENT_VALUE.MEASUREMENT_ID))
				)
				.where(RUN.REPO_ID.eq(repoId.getId().toString()))
				.and(RUN.ID.in(runIds))
				.and(RUN_MEASUREMENT.ERROR_MESSAGE.isNull())
				.forEach(r -> {
					String runId = r.value1();
					MeasurementName name = new MeasurementName(r.value2(), r.value3());
					double value = r.value4();

					tmpCommits.get(runId).getMeasurement(name).getValues().add(value);
				});
		}

		return tmpCommits.values();
	}

	public Map<Long, List<TmpCommit>> groupTmpCommits(Collection<TmpCommit> commits) {
		return commits.stream()
			.collect(Collectors.groupingBy(commit -> commitGrouper.getGroup(
				commit.getCommit().getAuthorDate().atZone(ZoneOffset.UTC)
			)));
	}

	public Optional<TmpCommit> getBestTmpCommit(Collection<TmpCommit> commits,
		MeasurementName measurementName) {

		// This assumes that the measurements all have the same interpretation. If they don't, the
		// returned commit is undefined.
		return commits.stream().reduce((a, b) -> {
			final TmpMeasurement aMeasurement = a.getMeasurement(measurementName);
			if (aMeasurement == null) {
				// b might or might not have the measurement, but a definitely doesn't
				return b;
			}

			final TmpMeasurement bMeasurement = b.getMeasurement(measurementName);
			if (bMeasurement == null) {
				// a definitely has the measurement
				return a;
			}

			final Interpretation interpretation = aMeasurement.getInterpretation();

			if (interpretation.equals(Interpretation.MORE_IS_BETTER)
				== (aMeasurement.getValue() >= bMeasurement.getValue())) {
				return a;
			} else {
				return b;
			}
		});
	}
}
