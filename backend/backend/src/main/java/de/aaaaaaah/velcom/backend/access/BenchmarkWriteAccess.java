package de.aaaaaaah.velcom.backend.access;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.jooq.codegen.db.Tables.DIMENSION;
import static org.jooq.codegen.db.Tables.MEASUREMENT;
import static org.jooq.codegen.db.Tables.TASK;
import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.impl.DSL.exists;

import com.github.benmanes.caffeine.cache.Cache;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.benchmark.NewMeasurement;
import de.aaaaaaah.velcom.backend.access.entities.benchmark.NewRun;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.caches.AvailableDimensionsCache;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.codegen.db.tables.records.DimensionRecord;
import org.jooq.codegen.db.tables.records.MeasurementRecord;
import org.jooq.codegen.db.tables.records.MeasurementValueRecord;
import org.jooq.codegen.db.tables.records.RunRecord;

/**
 * Provides read and write access to benchmark related entities such as runs and measurements.
 */
public class BenchmarkWriteAccess extends BenchmarkReadAccess {

	private final AvailableDimensionsCache availableDimensionsCache;

	public BenchmarkWriteAccess(DatabaseStorage databaseStorage, RepoReadAccess repoReadAccess,
		AvailableDimensionsCache availableDimensionsCache) {

		super(databaseStorage, repoReadAccess);
		this.availableDimensionsCache = availableDimensionsCache;
	}

	/**
	 * Inserts the specified run into the database. Also deletes the task with the same id from the
	 * task table in the same transaction.
	 *
	 * @param newRun the run to insert
	 */
	public void insertRun(NewRun newRun) {
		Run run = newRun.toRun();

		// 1. Insert run into database and delete associated task
		databaseStorage.acquireWriteTransaction(db -> {
			deleteTask(db, newRun.getId().toTaskId());
			updateDimensions(db, newRun);
			insertNewRun(db, newRun);
			insertNewMeasurements(db, newRun);
		});

		// 2. Insert run into cache
		synchronized (recentRunCache) {
			recentRunCache.add(run);

			// Need to sort again because this run may have been started before
			// the most recent run that is already in the cache
			recentRunCache.sort(recentRunCacheOrder);

			while (recentRunCache.size() > RECENT_RUN_CACHE_SIZE) {
				recentRunCache.remove(recentRunCache.size() - 1);
			}
		}

		// 3. If run has a commit source, insert into repoCache
		newRun.getSource().getLeft().ifPresent(commitSource -> {
			Cache<CommitHash, Run> cache = repoRunCache.computeIfAbsent(
				commitSource.getRepoId(),
				r -> buildRunCache(commitSource.getRepoId())
			);

			cache.put(commitSource.getHash(), run);
		});

		// 4. Update available dimensions cache
		// TODO: 21.10.20 Add new dimensions to cache more efficiently?
		newRun.getRepoId().ifPresent(availableDimensionsCache::invalidate);
	}

	private void deleteTask(DBWriteAccess db, TaskId taskId) {
		db.deleteFrom(TASK)
			.where(TASK.ID.eq(taskId.getIdAsString()))
			.execute();
	}

	private void updateDimensions(DBWriteAccess db, NewRun run) {
		Map<Dimension, DimensionRecord> dimensions = db.selectFrom(DIMENSION)
			.stream()
			.collect(toMap(
				record -> new Dimension(record.getBenchmark(), record.getMetric()),
				it -> it
			));

		run.getResult()
			.getRight()
			.orElse(List.of())
			.forEach(measurement -> {
				DimensionRecord record = dimensions.get(measurement.getDimension());

				if (record == null) {
					DimensionInfo info = new DimensionInfo(
						measurement.getDimension(),
						measurement.getUnit().orElse(null),
						measurement.getInterpretation().orElse(null)
					);

					dimensions.put(info.getDimension(), DimensionReadAccess.dimInfoToDimRecord(info));
				} else {
					measurement.getUnit()
						.map(Unit::getName)
						.filter(it -> record.getUnit().equals(it))
						.ifPresent(record::setUnit);

					measurement.getInterpretation()
						.map(Interpretation::getTextualRepresentation)
						.filter(it -> record.getInterpretation().equals(it))
						.ifPresent(record::setInterpretation);
				}
			});

		// Inserts newly generated dimensions and updates modified dimensions
		db.dsl().batchStore(dimensions.values()).execute();
	}

	private void insertNewRun(DBWriteAccess db, NewRun run) {
		RunRecord runRecord = new RunRecord(
			run.getIdAsString(),
			run.getAuthor(),
			run.getRunnerName(),
			run.getRunnerInfo(),
			run.getStartTime(),
			run.getStopTime(),
			run.getRepoId()
				.map(RepoId::getIdAsString)
				.orElse(null),
			run.getSource().getLeft()
				.map(CommitSource::getHash)
				.map(CommitHash::getHash)
				.orElse(null),
			run.getSource().getRight()
				.map(TarSource::getDescription)
				.orElse(null),
			run.getResult().getLeft()
				.map(RunError::getType)
				.map(RunErrorType::getTextualRepresentation)
				.orElse(null),
			run.getResult().getLeft()
				.map(RunError::getMessage)
				.orElse(null)
		);

		db.dsl().batchInsert(runRecord).execute();
	}

	private void insertNewMeasurements(DBWriteAccess db, NewRun run) {
		run.getResult()
			.getRight()
			.orElse(List.of())
			.forEach(measurement -> insertNewMeasurement(db, measurement));
	}

	private void insertNewMeasurement(DBWriteAccess db, NewMeasurement measurement) {
		String measurementId = UUID.randomUUID().toString();

		MeasurementRecord measurementRecord = new MeasurementRecord(
			measurementId,
			measurement.getRunId().getIdAsString(),
			measurement.getDimension().getBenchmark(),
			measurement.getDimension().getMetric(),
			measurement.getUnit()
				.map(Unit::getName)
				.orElse(null),
			measurement.getInterpretation()
				.map(Interpretation::getTextualRepresentation)
				.orElse(null),
			measurement.getContent()
				.getLeft()
				.map(MeasurementError::getErrorMessage)
				.orElse(null)
		);

		db.dsl().batchInsert(measurementRecord).execute();

		List<MeasurementValueRecord> records = measurement.getContent().getRight().stream()
			.flatMap(values -> values.getValues().stream()
				.map(value -> new MeasurementValueRecord(measurementId, value))
			).collect(toList());

		db.dsl().batchInsert(records).execute();
	}

	/**
	 * Delete all measurements of the specified name from a repo.
	 *
	 * <p> This is useful when a measurement becomes outdated or is renamed.
	 *
	 * @param repoId the repo to delete the measurements from
	 * @param dimension the name specifying which measurements to delete.
	 */
	public void deleteAllMeasurementsOfName(RepoId repoId, Dimension dimension) {
		// Update database
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.deleteFrom(MEASUREMENT)
				.where(
					exists(db.selectOne().from(RUN)
						.where(
							RUN.ID.eq(MEASUREMENT.RUN_ID)
								.and(RUN.REPO_ID.eq(repoId.getId().toString()))
						)
					)
						.and(MEASUREMENT.BENCHMARK.eq(dimension.getBenchmark()))
						.and(MEASUREMENT.METRIC.eq(dimension.getMetric()))
				)
				.execute();
		}

		// Repopulate recent run cache
		synchronized (recentRunCache) {
			recentRunCache.clear();
			recentRunCache.addAll(getRecentRuns(0, RECENT_RUN_CACHE_SIZE));
			recentRunCache.sort(recentRunCacheOrder);
		}

		// Update repo run cache
		Cache<CommitHash, Run> repoRunCache = this.repoRunCache.computeIfAbsent(repoId,
			i -> buildRunCache(repoId));

		List<CommitHash> copiedKeys = new ArrayList<>(repoRunCache.asMap().keySet());

		for (CommitHash copiedKey : copiedKeys) {
			Run run = repoRunCache.getIfPresent(copiedKey);
			if (run == null || run.getResult().isLeft()) {
				continue; // No measurements for this run => skip!
			}

			Collection<Measurement> measurements = run.getResult().getRight().get();

			// Check if target measurement name is one of the measurements
			List<Dimension> mNames = measurements.stream()
				.map(Measurement::getDimension)
				.collect(toList());

			if (!mNames.contains(dimension)) {
				continue; // run does not even contain deleted measurement => skip!
			}

			// This run contains the deleted measurement => need to invalidate run instance in cache
			// (since run was cached in repoRunCache, the run must have a repo source)
			repoRunCache.invalidate(run.getSource().getLeft().orElseThrow().getHash());
		}

		availableDimensionsCache.invalidate(repoId);
	}

	/**
	 * Delete all runs and their respective measurements of the specified repository.
	 *
	 * @param repoId the id of the repository
	 */
	public void deleteAllRunsOfRepo(RepoId repoId) {
		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.deleteFrom(RUN).where(RUN.REPO_ID.eq(repoId.getId().toString()));
		}

		// Invalidate recent run cache and reload it from database
		this.reloadRecentRunCache();

		availableDimensionsCache.invalidate(repoId);
	}

}
