package de.aaaaaaah.velcom.backend.newaccess;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.codegen.db.tables.RunMeasurement.RUN_MEASUREMENT;
import static org.jooq.codegen.db.tables.RunMeasurementValue.RUN_MEASUREMENT_VALUE;

import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.newaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.entities.Run;
import de.aaaaaaah.velcom.backend.newaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.newaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.codegen.db.tables.records.RunMeasurementRecord;
import org.jooq.codegen.db.tables.records.RunRecord;

/**
 * Provides read access to benchmark related entities such as runs and measurements.
 */
public class BenchmarkReadAccess {

	private final DatabaseStorage databaseStorage;

	public BenchmarkReadAccess(DatabaseStorage databaseStorage) {
		this.databaseStorage = Objects.requireNonNull(databaseStorage);
	}

	/**
	 * Gets the most recent runs ordered by their start time.
	 *
	 * @param skip how many recent runs to skip
	 * @param amount how many recent runs to collect
	 * @return a sorted list containing the recent runs
	 */
	public List<Run> getRecentRuns(int skip, int amount) {
		// Check cache
		// ...

		// Check database
		try (DSLContext db = databaseStorage.acquireContext()) {
			Map<String, RunRecord> runRecordMap = db.selectFrom(RUN)
				.orderBy(RUN.START_TIME.desc())
				.limit(skip, amount)
				.fetchMap(RUN.ID);

			List<Run> runList = loadRunData(db, runRecordMap);
			runList.sort(Comparator.comparing(Run::getStartTime));
			return runList;
		}
	}

	/**
	 * Gets the latest run for the given commit
	 *
	 * @param repoId the id of the repository
	 * @param commitHash the hash of the commit
	 * @return the latest run for the commit, or {@link Optional#empty()} if no run for that commit
	 * 	exists yet.
	 */
	public Optional<Run> getLatestRun(RepoId repoId, CommitHash commitHash) {
		Map<CommitHash, Run> resultMap = getLatestRuns(repoId, List.of(commitHash));
		return resultMap.containsKey(commitHash) ? Optional.of(resultMap.get(commitHash))
			: Optional.empty();
	}

	/**
	 * Gets the latest runs for the given commits.
	 *
	 * @param repoId the id of the repository that the commits are from
	 * @param commitHashes the hashes of the commits
	 * @return a map that maps each commit hash to its latest run
	 */
	public Map<CommitHash, Run> getLatestRuns(RepoId repoId, Collection<CommitHash> commitHashes) {
		Map<CommitHash, Run> resultMap = new HashMap<>();

		// Check cache
		// ...

		// Check database
		Set<String> uncachedCommitHashes = commitHashes.stream()
			.map(CommitHash::getHash)
			.collect(toSet());

		if (!uncachedCommitHashes.isEmpty()) {
			try (DSLContext db = databaseStorage.acquireContext()) {
				// Get all data from database
				Map<String, RunRecord> runRecordMap = db.selectFrom(RUN)
					.where(RUN.REPO_ID.eq(repoId.getId().toString()))
					.and(RUN.COMMIT_HASH.in(uncachedCommitHashes))
					.fetchMap(RUN.ID);

				loadRunData(db, runRecordMap).forEach(
					run -> resultMap.put(run.getCommitHash(), run));
			}
		}

		return resultMap;
	}

	/**
	 * Gets all available measurements for a given repository.
	 *
	 * @param repoId the id of the repository
	 * @return a collection of measurements
	 */
	public Collection<MeasurementName> getAvailableMeasurements(RepoId repoId) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.selectDistinct(RUN_MEASUREMENT.BENCHMARK, RUN_MEASUREMENT.METRIC)
				.from(RUN_MEASUREMENT)
				.join(RUN).on(RUN_MEASUREMENT.RUN_ID.eq(RUN.ID))
				.where(RUN.REPO_ID.eq(repoId.getId().toString()))
				.stream()
				.map(record -> new MeasurementName(record.value1(), record.value2()))
				.collect(toUnmodifiableSet());
		}
	}

	private List<Run> loadRunData(DSLContext db, Map<String, RunRecord> runRecordMap) {
		// 1.) Load measurements from database
		Map<String, RunMeasurementRecord> measurementRecordMap = db.selectFrom(RUN_MEASUREMENT)
			.where(RUN_MEASUREMENT.RUN_ID.in(runRecordMap.keySet()))
			.fetchMap(RUN_MEASUREMENT.ID);

		// 2.) Load measurement values from database
		Map<String, List<Double>> valueMap = db.selectFrom(RUN_MEASUREMENT_VALUE)
			.where(RUN_MEASUREMENT_VALUE.MEASUREMENT_ID.in(measurementRecordMap.keySet()))
			.fetchGroups(RUN_MEASUREMENT_VALUE.MEASUREMENT_ID, RUN_MEASUREMENT_VALUE.VALUE);

		// 3.) Create measurement entities
		Map<RunId, List<Measurement>> runToMeasurementMap = new HashMap<>();

		measurementRecordMap.values().forEach(measurementRecord -> {
			// Read basic measurement data
			RunRecord runRecord = runRecordMap.get(measurementRecord.getRunId());
			RunId runId = new RunId(UUID.fromString(runRecord.getId()));
			MeasurementName measurementName = new MeasurementName(
				measurementRecord.getBenchmark(), measurementRecord.getMetric()
			);

			final Measurement measurement;

			// Read measurement content
			if (measurementRecord.getErrorMessage() != null) {
				var measurementError = new MeasurementError(measurementRecord.getErrorMessage());
				measurement = new Measurement(runId, measurementName, measurementError);
			} else {
				List<Double> values = valueMap.get(measurementRecord.getId());
				Unit unit = new Unit(measurementRecord.getUnit());
				Interpretation interpretation = Interpretation.fromTextualRepresentation(
					measurementRecord.getInterpretation()
				);

				var measurementValues = new MeasurementValues(values, unit, interpretation);
				measurement = new Measurement(runId, measurementName, measurementValues);
			}

			// Insert measurement into map
			if (!runToMeasurementMap.containsKey(runId)) {
				runToMeasurementMap.put(runId, new ArrayList<>());
			}

			runToMeasurementMap.get(runId).add(measurement);
		});

		// 4.) Create run entities
		return runRecordMap.values().stream()
			.map(runRecord -> {
				RunId runId = new RunId(UUID.fromString(runRecord.getId()));
				RepoId repoId = new RepoId(UUID.fromString(runRecord.getRepoId()));
				CommitHash commitHash = new CommitHash(runRecord.getCommitHash());

				if (runRecord.getErrorMessage() != null) {
					List<Measurement> measurements = runToMeasurementMap.get(runId);
					return new Run(
						runId,
						repoId,
						commitHash,
						runRecord.getStartTime().toInstant(),
						runRecord.getStopTime().toInstant(),
						measurements
					);
				} else {
					return new Run(
						runId,
						repoId,
						commitHash,
						runRecord.getStartTime().toInstant(),
						runRecord.getStopTime().toInstant(),
						runRecord.getErrorMessage()
					);
				}
			})
			.collect(toList());
	}

}
