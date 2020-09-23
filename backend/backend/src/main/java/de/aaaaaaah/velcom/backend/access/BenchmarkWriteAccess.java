package de.aaaaaaah.velcom.backend.access;

import static java.util.stream.Collectors.toList;
import static org.jooq.codegen.db.Tables.MEASUREMENT;
import static org.jooq.codegen.db.Tables.MEASUREMENT_VALUE;
import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.impl.DSL.exists;

import com.github.benmanes.caffeine.cache.Cache;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunError;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
import de.aaaaaaah.velcom.backend.access.entities.benchmark.NewMeasurement;
import de.aaaaaaah.velcom.backend.access.entities.benchmark.NewRun;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jooq.codegen.db.tables.records.MeasurementRecord;
import org.jooq.codegen.db.tables.records.RunRecord;

public class BenchmarkWriteAccess extends BenchmarkReadAccess {

	private final TaskWriteAccess taskAccess;

	public BenchmarkWriteAccess(DatabaseStorage databaseStorage, RepoReadAccess repoReadAccess,
		TaskWriteAccess taskAccess) {
		super(databaseStorage, repoReadAccess);
		this.taskAccess = taskAccess;
	}

	/**
	 * Inserts the specified run into the database.
	 *
	 * @param newRun the run to insert
	 */
	public void insertRun(NewRun newRun) {
		Run run = newRun.toRun();

		// Insert run into database and delete associated task
		databaseStorage.acquireWriteTransaction(db -> {
			// 0.) Delete associated task
			TaskId taskId = new TaskId(newRun.getId().getId());
			taskAccess.deleteTasks(List.of(taskId), db);

			// 1.) Insert run
			RunRecord runRecord = db.newRecord(RUN);
			runRecord.setId(newRun.getId().getId().toString());
			runRecord.setAuthor(newRun.getAuthor());
			runRecord.setRunnerName(newRun.getRunnerName());
			runRecord.setRunnerInfo(newRun.getRunnerInfo());
			runRecord.setStartTime(newRun.getStartTime());
			runRecord.setStopTime(newRun.getStopTime());
			runRecord.setRepoId(
				newRun.getRepoId().map(RepoId::getId)
					.map(UUID::toString)
					.orElse(null)
			);

			if (newRun.getSource().isLeft()) {
				runRecord.setCommitHash(newRun.getSource().getLeft().get().getHash().getHash());
			} else {
				runRecord.setTarDesc(newRun.getSource().getRight().get().getDescription());
			}

			if (newRun.getResult().isLeft()) {
				RunError error = newRun.getResult().getLeft().orElseThrow();
				runRecord.setError(error.getMessage());
				runRecord.setErrorType(error.getType().getTextualRepresentation());
				runRecord.insert();
			} else {
				runRecord.insert();

				Collection<NewMeasurement> measurements = newRun.getResult().getRight().orElseThrow();
				for (NewMeasurement measurement : measurements) {
					insertMeasurement(db, measurement);
				}
			}
		});

		// 2.) Invalidate dimension cache for repo
		newRun.getSource().getLeft().ifPresent(source -> dimensionCache.remove(source.getRepoId()));

		// 3.) Insert run into cache
		synchronized (recentRunCache) {
			recentRunCache.add(run);

			// Need to sort again because this run may have been started before
			// the most recent run that is already in the cache
			recentRunCache.sort(recentRunCacheOrder);

			while (recentRunCache.size() > RECENT_RUN_CACHE_SIZE) {
				recentRunCache.remove(recentRunCache.size() - 1);
			}
		}

		// 4.) If run has a commit source, insert into repoCache
		newRun.getSource().getLeft().ifPresent(commitSource -> {
			Cache<CommitHash, Run> cache = repoRunCache.computeIfAbsent(
				commitSource.getRepoId(),
				r -> buildRunCache(commitSource.getRepoId())
			);

			cache.put(commitSource.getHash(), run);
		});
	}

	private void insertMeasurement(DBWriteAccess db, NewMeasurement measurement) {
		String measurementId = UUID.randomUUID().toString();

		MeasurementRecord measurementRecord = db.newRecord(MEASUREMENT);
		measurementRecord.setId(measurementId);
		measurementRecord.setRunId(measurement.getRunId().getId().toString());
		measurementRecord.setBenchmark(measurement.getDimension().getBenchmark());
		measurementRecord.setMetric(measurement.getDimension().getMetric());
		measurement.getUnit().ifPresent(unit -> measurementRecord.setUnit(unit.getName()));
		measurement.getInterpretation().ifPresent(interpretation -> measurementRecord
			.setInterpretation(interpretation.getTextualRepresentation()));

		if (measurement.getContent().isLeft()) {
			MeasurementError error = measurement.getContent().getLeft().orElseThrow();

			measurementRecord.setError(error.getErrorMessage());
			measurementRecord.insert();
		} else {
			MeasurementValues values = measurement.getContent().getRight().orElseThrow();
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

		updateDimensions(measurement);
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

		// Invalidate measurement cache
		dimensionCache.remove(repoId);

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

		// Invalidate measurement cache
		dimensionCache.remove(repoId);

		// Invalidate recent run cache and reload it from database
		this.reloadRecentRunCache();
	}

}
