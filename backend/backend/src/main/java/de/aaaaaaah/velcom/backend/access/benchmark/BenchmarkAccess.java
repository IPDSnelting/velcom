package de.aaaaaaah.velcom.backend.access.benchmark;

import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.codegen.db.tables.RunMeasurement.RUN_MEASUREMENT;
import static org.jooq.codegen.db.tables.RunMeasurementValue.RUN_MEASUREMENT_VALUE;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.selectFrom;

import de.aaaaaaah.velcom.backend.access.AccessLayer;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.util.Either;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep2;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.codegen.db.tables.records.RunMeasurementRecord;
import org.jooq.codegen.db.tables.records.RunMeasurementValueRecord;
import org.jooq.codegen.db.tables.records.RunRecord;

/**
 * This class abstracts away access to the benchmark results such as runs and measurements.
 */
public class BenchmarkAccess {

	private final AccessLayer accessLayer;
	private final DatabaseStorage databaseStorage;

	/**
	 * This constructor also registers the {@link BenchmarkAccess} in the accessLayer.
	 *
	 * @param accessLayer the {@link AccessLayer} to register with
	 * @param databaseStorage a database storage
	 */
	public BenchmarkAccess(AccessLayer accessLayer, DatabaseStorage databaseStorage) {
		this.accessLayer = accessLayer;
		this.databaseStorage = databaseStorage;

		accessLayer.registerBenchmarkAccess(this);
	}

	// Run ac

	/*
	Basic querying
	 */

	private Run runFromRecord(RunRecord runRecord) {
		return new Run(
			accessLayer.getBenchmarkAccess(),
			accessLayer.getCommitAccess(),
			accessLayer.getRepoAccess(),
			new RunId(UUID.fromString(runRecord.getId())),
			new RepoId(UUID.fromString(runRecord.getRepoId())),
			new CommitHash(runRecord.getCommitHash()),
			runRecord.getStartTime().toInstant(),
			runRecord.getStopTime().toInstant(),
			runRecord.getErrorMessage()
		);
	}

	public Run getRun(RunId runId) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			final RunRecord runRecord = db.fetchOne(RUN, RUN.ID.eq(runId.getId().toString()));

			if (runRecord == null) {
				throw new NoSuchRunException(runId);
			}

			return runFromRecord(runRecord);
		}
	}

	public Optional<Run> getLatestRunOf(Commit commit) {
		return getLatestRunOf(commit.getRepoId(), commit.getHash());
	}

	public Optional<Run> getLatestRunOf(RepoId repoId, CommitHash commitHash) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			final Optional<RunRecord> runRecord = db.selectFrom(RUN)
				.where(RUN.REPO_ID.eq(repoId.getId().toString()))
				.and(RUN.COMMIT_HASH.eq(commitHash.getHash()))
				.orderBy(RUN.START_TIME.desc())
				.limit(1)
				.fetchOptional();

			return runRecord.map(this::runFromRecord);
		}
	}

	/*
	Advanced operations
	 */

	/**
	 * Add a new successful run to the db.
	 *
	 * @param repoId the repo the commit is in
	 * @param commitHash the hash of the benchmarked commit
	 * @param startTime the time the benchmark script execution was started
	 * @param stopTime the time the benchmark script finished executing
	 * @return the newly created {@link Run}
	 */
	public Run addRun(RepoId repoId, CommitHash commitHash, Instant startTime, Instant stopTime) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			final RunRecord runRecord = db.newRecord(RUN);

			runRecord.setId(UUID.randomUUID().toString());
			runRecord.setRepoId(repoId.getId().toString());
			runRecord.setCommitHash(commitHash.getHash());
			runRecord.setStartTime(Timestamp.from(startTime));
			runRecord.setStopTime(Timestamp.from(stopTime));

			runRecord.insert();
			return runFromRecord(runRecord);
		}
	}

	/**
	 * Add a new failed run to the db.
	 *
	 * @param repoId the repo the commit is in
	 * @param commitHash the hash of the benchmarked commit
	 * @param startTime the time the benchmark script execution was started
	 * @param stopTime the time the benchmark script finished executing
	 * @param errorMessage the message with which the run failed
	 * @return the newly created {@link Run}
	 */
	public Run addFailedRun(RepoId repoId, CommitHash commitHash, Instant startTime,
		Instant stopTime, String errorMessage) {

		try (DSLContext db = databaseStorage.acquireContext()) {
			final RunRecord runRecord = db.newRecord(RUN);

			runRecord.setId(UUID.randomUUID().toString());
			runRecord.setRepoId(repoId.getId().toString());
			runRecord.setCommitHash(commitHash.getHash());
			runRecord.setStartTime(Timestamp.from(startTime));
			runRecord.setStopTime(Timestamp.from(stopTime));
			runRecord.setErrorMessage(errorMessage);

			runRecord.insert();
			return runFromRecord(runRecord);
		}
	}

	/**
	 * Add a new successful measurement to an existing run.
	 *
	 * @param runId the run's id
	 * @param measurementName the measurement identification
	 * @param values the measured values
	 * @param interpretation the values' interpretation
	 * @param unit the values' unit
	 * @return the newly created {@link Measurement}
	 */
	public Measurement addMeasurement(RunId runId, MeasurementName measurementName,
		List<Double> values, Interpretation interpretation, Unit unit) {

		try (DSLContext db = databaseStorage.acquireContext()) {
			// Insert new measurement
			final UUID measurement_id = UUID.randomUUID();
			final RunMeasurementRecord runMeasurementRecord = db.newRecord(RUN_MEASUREMENT);

			runMeasurementRecord.setId(measurement_id.toString());
			runMeasurementRecord.setRunId(runId.getId().toString());
			runMeasurementRecord.setBenchmark(measurementName.getBenchmark());
			runMeasurementRecord.setMetric(measurementName.getMetric());
			runMeasurementRecord.setUnit(unit.getName());
			runMeasurementRecord.setInterpretation(interpretation.getTextualRepresentation());

			runMeasurementRecord.insert();

			// Insert individual measurement values
			final InsertValuesStep2<RunMeasurementValueRecord, String, Double> step = db.insertInto(
				RUN_MEASUREMENT_VALUE)
				.columns(RUN_MEASUREMENT_VALUE.MEASUREMENT_ID, RUN_MEASUREMENT_VALUE.VALUE);
			values.forEach(value -> step.values(measurement_id.toString(), value));
			step.execute();

			return new Measurement(
				accessLayer.getBenchmarkAccess(),
				runId,
				measurementName,
				Either.ofRight(new MeasurementValues(values, unit, interpretation))
			);
		}
	}


	/**
	 * Add a new failed measurement to an existing run.
	 *
	 * @param runId the run's id
	 * @param measurementName the measurement identification
	 * @param errorMessage the message with which the specific measurement failed
	 * @return the newly created {@link Measurement}
	 */
	public Measurement addFailedMeasurement(RunId runId, MeasurementName measurementName,
		String errorMessage) {

		try (DSLContext db = databaseStorage.acquireContext()) {
			// Insert new measurement
			final UUID measurement_id = UUID.randomUUID();
			final RunMeasurementRecord runMeasurementRecord = db.newRecord(RUN_MEASUREMENT);

			runMeasurementRecord.setId(measurement_id.toString());
			runMeasurementRecord.setRunId(runId.getId().toString());
			runMeasurementRecord.setBenchmark(measurementName.getBenchmark());
			runMeasurementRecord.setMetric(measurementName.getMetric());
			runMeasurementRecord.setErrorMessage(errorMessage);

			runMeasurementRecord.insert();

			return new Measurement(
				accessLayer.getBenchmarkAccess(),
				runId,
				measurementName,
				Either.ofLeft(new MeasurementError(errorMessage))
			);
		}
	}

	/**
	 * Streams all runs sorted by their start time where run with the most recent start time is the
	 * first element in the stream.
	 *
	 * <p>Note that this stream must be closed after it is no longer being used.</p>
	 *
	 * @return sorted stream containing all runs
	 */
	public Stream<Run> getRecentRuns() {
		DSLContext db = databaseStorage.acquireContext();

		try {
			return db.selectFrom(RUN)
				.orderBy(RUN.START_TIME)
				.fetchStream()
				.map(this::runFromRecord)
				.onClose(db::close);
		} catch (Exception e) {
			db.close();
			throw e; // Thanketh the gods who taketh away the compiler exceptions.
		}
	}

	/**
	 * Delete all measurements of the specified name from a repo.
	 *
	 * <p> This is useful when a measurement becomes outdated or is renamed.
	 *
	 * @param repoId the repo to delete the measurements from
	 * @param measurementName the name specifying which measurements to delete.
	 */
	public void deleteAllMeasurementsOfName(RepoId repoId, MeasurementName measurementName) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.deleteFrom(RUN_MEASUREMENT)
				.where(
					exists(db.selectOne().from(RUN)
						.where(RUN.ID.eq(RUN_MEASUREMENT.RUN_ID)
							.and(RUN.REPO_ID.eq(repoId.getId().toString())))
					)
						.and(RUN_MEASUREMENT.BENCHMARK.eq(measurementName.getBenchmark()))
						.and(RUN_MEASUREMENT.METRIC.eq(measurementName.getMetric())))
				.execute();
		}
	}

	public Collection<Measurement> getMeasurements(RunId runId) {
		Map<String, RunMeasurementRecord> measurementRecordMap; // key: run_measurement_id
		Map<String, Result<Record2<String, Double>>> valueMap; // key: run_measurement_id

		try (DSLContext db = databaseStorage.acquireContext()) {
			// 1.) Load measurement records
			measurementRecordMap = db.selectFrom(RUN_MEASUREMENT)
				.where(RUN_MEASUREMENT.RUN_ID.eq(runId.getId().toString()))
				.fetch()
				.intoMap(RUN_MEASUREMENT.ID);

			// 2.) Load measurement values
			valueMap = db.select(
				RUN_MEASUREMENT_VALUE.MEASUREMENT_ID, RUN_MEASUREMENT_VALUE.VALUE)
				.from(RUN_MEASUREMENT_VALUE
					.join(RUN_MEASUREMENT)
					.on(RUN_MEASUREMENT_VALUE.MEASUREMENT_ID.eq(RUN_MEASUREMENT.ID))
				)
				.where(RUN_MEASUREMENT.RUN_ID.eq(runId.getId().toString()))
				.fetch()
				.intoGroups(RUN_MEASUREMENT_VALUE.MEASUREMENT_ID);
		}

		// 3.) Construct measurement entities
		List<Measurement> measurements = new ArrayList<>(measurementRecordMap.size());

		for (RunMeasurementRecord measureRecord : measurementRecordMap.values()) {
			MeasurementName name = new MeasurementName(
				measureRecord.getBenchmark(),
				measureRecord.getMetric()
			);

			if (measureRecord.getErrorMessage() != null) {
				// measurement failed
				var error = new MeasurementError(measureRecord.getErrorMessage());
				var measurement = new Measurement(this, runId, name, error);

				measurements.add(measurement);
			} else {
				// measurement succeeded
				var unit = new Unit(measureRecord.getUnit());

				String interpStr = measureRecord.getInterpretation();
				Interpretation interp = Interpretation.fromTextualRepresentation(interpStr);

				final List<Double> valueList;

				if (valueMap.containsKey(measureRecord.getId())) {
					valueList = valueMap.get(measureRecord.getId())
						.map(Record2::value2);
				} else {
					valueList = Collections.emptyList();
				}

				var values = new MeasurementValues(valueList, unit, interp);
				var measurement = new Measurement(this, runId, name, values);

				measurements.add(measurement);
			}
		}

		return measurements;
	}

	/**
	 * @param repoId the repo's id
	 * @return all {@link MeasurementName}s of which at least one measurement exists for this repo
	 */
	public Collection<MeasurementName> getAvailableMeasurements(RepoId repoId) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			return db.selectDistinct(RUN_MEASUREMENT.BENCHMARK, RUN_MEASUREMENT.METRIC)
				.from(RUN_MEASUREMENT)
				.where(exists(selectFrom(RUN)
					.where(RUN.REPO_ID.eq(repoId.getId().toString()))
					.and(RUN.ID.eq(RUN_MEASUREMENT.RUN_ID))
				))
				.stream()
				.map(record -> new MeasurementName(record.value1(), record.value2()))
				.collect(Collectors.toUnmodifiableSet());
		}
	}

}
