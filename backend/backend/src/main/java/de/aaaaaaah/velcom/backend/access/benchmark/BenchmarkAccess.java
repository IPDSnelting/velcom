package de.aaaaaaah.velcom.backend.access.benchmark;

import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.codegen.db.tables.RunMeasurement.RUN_MEASUREMENT;
import static org.jooq.codegen.db.tables.RunMeasurementValue.RUN_MEASUREMENT_VALUE;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.max;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep2;
import org.jooq.Record1;
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

	/**
	 * Get the IDs of the latest runs of a list of commits. Preserves the ordering of the commits.
	 */
	public List<RunId> getLatestRunIds(RepoId repoId, List<CommitHash> commitHashes) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			final Set<String> hashSet = commitHashes.stream()
				.map(CommitHash::getHash)
				.collect(Collectors.toUnmodifiableSet());

			final Map<CommitHash, RunId> runIdsByHash = db
				.select(RUN.COMMIT_HASH, RUN.ID, max(RUN.START_TIME))
				.from(RUN)
				.where(RUN.REPO_ID.eq(repoId.getId().toString()))
				.and(RUN.COMMIT_HASH.in(hashSet))
				.groupBy(RUN.COMMIT_HASH)
				.stream()
				.collect(Collectors.toMap(
					record -> new CommitHash(record.value1()),
					record -> new RunId(UUID.fromString(record.value2()))
				));

			return commitHashes.stream()
				.map(runIdsByHash::get)
				.filter(Objects::nonNull)
				.collect(Collectors.toUnmodifiableList());
		}
	}

	public Optional<RunId> getLatestRunId(RepoId repoId, CommitHash commitHash) {
		final List<RunId> runIds = getLatestRunIds(repoId, List.of(commitHash));

		if (runIds.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(runIds.get(0));
		}
	}

	/**
	 * Get the runs specified by the run IDs. Preserves the ordering of the IDs.
	 */
	public List<Run> getRuns(List<RunId> runIds) {
		final Set<String> runIdsAsStrings = runIds.stream()
			.map(RunId::getId)
			.map(UUID::toString)
			.collect(Collectors.toUnmodifiableSet());

		try (DSLContext db = databaseStorage.acquireContext()) {
			final Map<RunId, TmpRun> runs = db.selectFrom(RUN)
				.where(RUN.ID.in(runIdsAsStrings))
				.stream()
				.map(record -> new TmpRun(
					new RunId(UUID.fromString(record.getId())),
					new RepoId(UUID.fromString(record.getRepoId())),
					new CommitHash(record.getCommitHash()),
					record.getStartTime().toInstant(),
					record.getStopTime().toInstant(),
					record.getErrorMessage()
				))
				.collect(Collectors.toMap(TmpRun::getId, run -> run));

			db.selectFrom(RUN_MEASUREMENT)
				.where(RUN_MEASUREMENT.RUN_ID.in(runIds))
				.stream()
				.map(record -> new TmpMeasurement(
					new RunId(UUID.fromString(record.getRunId())),
					new MeasurementName(record.getBenchmark(), record.getMetric()),
					(record.getUnit() != null) ? new Unit(record.getUnit()) : null,
					(record.getInterpretation() != null)
						? Interpretation.fromTextualRepresentation(record.getInterpretation())
						: null,
					record.getErrorMessage()
				))
				.forEach(measurement -> runs.get(measurement.getRunId())
					.getMeasurements()
					.put(measurement.getMeasurementName(), measurement)
				);

			db.select(RUN_MEASUREMENT.RUN_ID, RUN_MEASUREMENT.BENCHMARK, RUN_MEASUREMENT.METRIC,
				RUN_MEASUREMENT_VALUE.VALUE)
				.from(RUN_MEASUREMENT)
				.join(RUN_MEASUREMENT_VALUE)
				.on(RUN_MEASUREMENT.ID.eq(RUN_MEASUREMENT_VALUE.MEASUREMENT_ID))
				.where(RUN_MEASUREMENT.RUN_ID.in(runIds))
				.forEach(record -> {
					final RunId runId = new RunId(UUID.fromString(record.value1()));
					final MeasurementName measurementName = new MeasurementName(record.value2(),
						record.value3());
					final double value = record.value4();
					runs.get(runId).getMeasurements().get(measurementName).getValues().add(value);
				});

			return runIds.stream()
				.map(runs::get)
				.filter(Objects::nonNull)
				.map(run -> run.toRun(
					accessLayer.getBenchmarkAccess(),
					accessLayer.getCommitAccess(),
					accessLayer.getRepoAccess()
				))
				.collect(Collectors.toUnmodifiableList());
		}
	}

	public Run getRun(RunId runId) {
		final List<Run> runs = getRuns(List.of(runId));

		if (runs.isEmpty()) {
			throw new NoSuchRunException(runId);
		}

		return runs.get(0);
	}

	public Optional<Run> getLatestRunOf(Commit commit) {
		return getLatestRunId(commit.getRepoId(), commit.getHash()).map(this::getRun);
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
	public RunId addRun(RepoId repoId, CommitHash commitHash, Instant startTime, Instant stopTime) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			final RunId runId = new RunId(UUID.randomUUID());
			final RunRecord runRecord = db.newRecord(RUN);

			runRecord.setId(runId.getId().toString());
			runRecord.setRepoId(repoId.getId().toString());
			runRecord.setCommitHash(commitHash.getHash());
			runRecord.setStartTime(Timestamp.from(startTime));
			runRecord.setStopTime(Timestamp.from(stopTime));

			runRecord.insert();
			return runId;
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
	public RunId addFailedRun(RepoId repoId, CommitHash commitHash, Instant startTime,
		Instant stopTime, String errorMessage) {

		try (DSLContext db = databaseStorage.acquireContext()) {
			final RunId runId = new RunId(UUID.randomUUID());
			final RunRecord runRecord = db.newRecord(RUN);

			runRecord.setId(runId.getId().toString());
			runRecord.setRepoId(repoId.getId().toString());
			runRecord.setCommitHash(commitHash.getHash());
			runRecord.setStartTime(Timestamp.from(startTime));
			runRecord.setStopTime(Timestamp.from(stopTime));
			runRecord.setErrorMessage(errorMessage);

			runRecord.insert();
			return runId;
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
	 * Streams all run ids sorted by the run's start time (where run with the most recent start time
	 * is the first element in the stream).
	 *
	 * <p>Note that this stream must be closed after it is no longer being used.</p>
	 *
	 * @return sorted stream containing all runs
	 */
	public Stream<RunId> getRecentRunIds() {
		DSLContext db = databaseStorage.acquireContext();

		try {
			return db.select(RUN.ID)
				.from(RUN)
				.orderBy(RUN.START_TIME.desc())
				.stream()
				.map(Record1::value1)
				.map(UUID::fromString)
				.map(RunId::new)
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
