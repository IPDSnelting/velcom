package de.aaaaaaah.velcom.backend.access;

import static java.util.stream.Collectors.toList;
import static org.jooq.codegen.db.Tables.MEASUREMENT;
import static org.jooq.codegen.db.Tables.MEASUREMENT_VALUE;
import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.impl.DSL.exists;

import com.github.benmanes.caffeine.cache.Cache;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunError;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.codegen.db.tables.records.MeasurementRecord;
import org.jooq.codegen.db.tables.records.RunRecord;

public class BenchmarkWriteAccess extends BenchmarkReadAccess {

	private final TaskWriteAccess taskAccess;

	public BenchmarkWriteAccess(DatabaseStorage databaseStorage, TaskWriteAccess taskAccess) {
		super(databaseStorage);
		this.taskAccess = taskAccess;
	}

	/**
	 * Inserts the specified run into the database.
	 *
	 * @param run the run to insert
	 */
	public void insertRun(Run run) {
		// Insert run into database and delete associated task
		databaseStorage.acquireTransaction(db -> {
			// 0.) Delete associated task
			TaskId taskId = new TaskId(run.getId().getId());
			taskAccess.deleteTasks(List.of(taskId), db);

			// 1.) Insert run
			RunRecord runRecord = db.newRecord(RUN);
			runRecord.setId(run.getId().getId().toString());
			runRecord.setAuthor(run.getAuthor());
			runRecord.setRunnerName(run.getRunnerName());
			runRecord.setRunnerInfo(run.getRunnerInfo());
			runRecord.setStartTime(Timestamp.from(run.getStartTime()));
			runRecord.setStopTime(Timestamp.from(run.getStopTime()));

			run.getRepoSource().ifPresent(repoSource -> {
				runRecord.setRepoId(repoSource.getRepoId().getId().toString());
				runRecord.setCommitHash(repoSource.getHash().getHash());
			});

			if (run.getResult().isRight()) {
				RunError error = run.getResult().getRight().orElseThrow();
				runRecord.setError(error.getMessage());
				runRecord.setErrorType(error.getType().getTextualRepresentation());
			} else {
				Collection<Measurement> measurements = run.getResult().getLeft().orElseThrow();

				for (Measurement measurement : measurements) {
					insertMeasurement(db, measurement);
				}
			}

			runRecord.insert();
		});

		// 2.) Insert run into recent cache
		synchronized (recentRunCache) {
			recentRunCache.add(run);

			// Need to sort again because this run may have been started before
			// the most recent run that is already in the cache
			recentRunCache.sort(recentRunCacheOrder);

			while (recentRunCache.size() > RECENT_RUN_CACHE_SIZE) {
				recentRunCache.remove(recentRunCache.size() - 1);
			}
		}

		// 4.) If run is associated with a repository, insert into repoCache
		run.getRepoSource().ifPresent(repoSource -> {
			Cache<CommitHash, Run> cache = repoRunCache.computeIfAbsent(
				repoSource.getRepoId(),
				r -> RUN_CACHE_BUILDER.build()
			);

			cache.put(repoSource.getHash(), run);
		});
	}

	private void insertMeasurement(DSLContext db, Measurement measurement) {
		String measurementId = UUID.randomUUID().toString();

		MeasurementRecord measurementRecord = db.newRecord(MEASUREMENT);
		measurementRecord.setId(measurementId);
		measurementRecord.setRunId(measurement.getRunId().getId().toString());
		measurementRecord.setBenchmark(measurement.getMeasurementName().getBenchmark());
		measurementRecord.setMetric(measurement.getMeasurementName().getMetric());

		if (measurement.getContent().isLeft()) {
			MeasurementError error = measurement.getContent().getLeft().orElseThrow();

			measurementRecord.setError(error.getErrorMessage());
			measurementRecord.insert();
		} else {
			MeasurementValues values = measurement.getContent().getRight().orElseThrow();

			measurementRecord.setUnit(values.getUnit().getName());
			measurementRecord.setInterpretation(
				values.getInterpretation().getTextualRepresentation()
			);

			measurementRecord.insert();

			// Insert values into database
			var valuesInsertStep = db.insertInto(MEASUREMENT_VALUE)
				.columns(
					MEASUREMENT_VALUE.MEASUREMENT_ID,
					MEASUREMENT_VALUE.VALUE
				);

			values.getValues().forEach(value -> valuesInsertStep.values(
				measurementId, value
			));

			valuesInsertStep.execute();
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
		// Update database
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.deleteFrom(MEASUREMENT)
				.where(
					exists(db.selectOne().from(RUN)
						.where(
							RUN.ID.eq(MEASUREMENT.RUN_ID)
								.and(RUN.REPO_ID.eq(repoId.getId().toString()))
						)
					)
						.and(MEASUREMENT.BENCHMARK.eq(measurementName.getBenchmark()))
						.and(MEASUREMENT.METRIC.eq(measurementName.getMetric()))
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
			i -> RUN_CACHE_BUILDER.build());

		List<CommitHash> copiedKeys = new ArrayList<>(repoRunCache.asMap().keySet());

		for (CommitHash copiedKey : copiedKeys) {
			Run run = repoRunCache.getIfPresent(copiedKey);
			if (run == null || run.getResult().isRight()) {
				continue; // No measurements for this run => skip!
			}

			Collection<Measurement> measurements = run.getResult().getLeft().get();

			// Check if target measurement name is one of the measurements
			List<MeasurementName> mNames = measurements.stream()
				.map(Measurement::getMeasurementName)
				.collect(toList());

			if (!mNames.contains(measurementName)) {
				continue; // run does not even contain deleted measurement => skip!
			}

			// This run contains the deleted measurement => need to invalidate run instance in cache
			// (since run was cached in repoRunCache, the run must have a repo source)
			repoRunCache.invalidate(run.getRepoSource().orElseThrow().getHash());
		}
	}

	/**
	 * Delete all runs and their respective measurements of the specified repository
	 *
	 * @param repoId the id of the repository
	 */
	public void deleteAllRunsOfRepo(RepoId repoId) {
		try (DSLContext db = databaseStorage.acquireContext()) {
			db.deleteFrom(RUN).where(RUN.REPO_ID.eq(repoId.getId().toString()));
		}

		// Invalidate recent run cache and reload it from database
		this.reloadRecentRunCache();
	}

}
