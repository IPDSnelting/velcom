package de.aaaaaaah.velcom.backend.access;

import static java.util.stream.Collectors.toList;
import static org.jooq.codegen.db.Tables.RUN_MEASUREMENT_VALUE;
import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.codegen.db.tables.RunMeasurement.RUN_MEASUREMENT;
import static org.jooq.impl.DSL.exists;

import com.github.benmanes.caffeine.cache.Cache;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.codegen.db.tables.records.RunMeasurementRecord;
import org.jooq.codegen.db.tables.records.RunRecord;
import org.jooq.impl.DSL;

public class BenchmarkWriteAccess extends BenchmarkReadAccess {

	public BenchmarkWriteAccess(DatabaseStorage databaseStorage) {
		super(databaseStorage);
	}

	/**
	 * Inserts the specified run into the database.
	 *
	 * @param run the run to insert
	 */
	public void insertRun(Run run) {
		try (DSLContext initialContext = databaseStorage.acquireContext()) {
			initialContext.transaction(configuration -> {
				try (DSLContext transaction = DSL.using(configuration)) {
					// 1.) Insert run into database
					RunRecord runRecord = transaction.newRecord(RUN);
					runRecord.setId(run.getId().getId().toString());
					runRecord.setRepoId(run.getRepoId().getId().toString());
					runRecord.setCommitHash(run.getCommitHash().getHash());
					runRecord.setStartTime(Timestamp.from(run.getStartTime()));
					runRecord.setStopTime(Timestamp.from(run.getStopTime()));
					runRecord.setErrorMessage(run.getErrorMessage().orElse(null));

					runRecord.insert();

					// 2.) Insert measurements into database
					if (run.getMeasurements().isPresent()) {
						Collection<Measurement> measurements = run.getMeasurements().get();

						for (Measurement measurement : measurements) {
							String measurementId = UUID.randomUUID().toString();

							RunMeasurementRecord measurementRecord = transaction.newRecord(
								RUN_MEASUREMENT);
							measurementRecord.setId(measurementId);
							measurementRecord.setRunId(run.getId().getId().toString());
							measurementRecord.setBenchmark(
								measurement.getMeasurementName().getBenchmark());
							measurementRecord.setMetric(
								measurement.getMeasurementName().getMetric());

							if (measurement.getContent().isLeft()) {
								MeasurementError error = measurement.getContent()
									.getLeft()
									.orElseThrow();

								measurementRecord.setErrorMessage(error.getErrorMessage());
								measurementRecord.insert();
							} else {
								MeasurementValues values = measurement.getContent()
									.getRight()
									.orElseThrow();

								measurementRecord.setUnit(values.getUnit().getName());
								measurementRecord.setInterpretation(
									values.getInterpretation().getTextualRepresentation()
								);
								measurementRecord.insert();

								// 2.1.) Insert values into database
								var valuesInsertStep = transaction.insertInto(RUN_MEASUREMENT_VALUE)
									.columns(
										RUN_MEASUREMENT_VALUE.MEASUREMENT_ID,
										RUN_MEASUREMENT_VALUE.VALUE
									);

								values.getValues().forEach(value -> valuesInsertStep.values(
									measurementId, value
								));

								valuesInsertStep.execute();
							}
						}
					}
				}
			});

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

			final Cache<CommitHash, Run> cache = runCache.computeIfAbsent(run.getRepoId(),
				r -> RUN_CACHE_BUILDER.build()
			);

			cache.put(run.getCommitHash(), run);
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
			db.deleteFrom(RUN_MEASUREMENT)
				.where(
					exists(db.selectOne().from(RUN)
						.where(
							RUN.ID.eq(RUN_MEASUREMENT.RUN_ID)
								.and(RUN.REPO_ID.eq(repoId.getId().toString()))
						)
					)
						.and(RUN_MEASUREMENT.BENCHMARK.eq(measurementName.getBenchmark()))
						.and(RUN_MEASUREMENT.METRIC.eq(measurementName.getMetric()))
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
		Cache<CommitHash, Run> repoRunCache = runCache.computeIfAbsent(repoId,
			i -> RUN_CACHE_BUILDER.build());

		List<CommitHash> copiedKeys = new ArrayList<>(repoRunCache.asMap().keySet());

		for (CommitHash copiedKey : copiedKeys) {
			Run run = repoRunCache.getIfPresent(copiedKey);
			if (run == null || run.getMeasurements().isEmpty()) {
				continue;
			} // Skip!
			Collection<Measurement> measurements = run.getMeasurements().get();

			// Check if target measurement name is one of the measurements
			List<MeasurementName> mNames = measurements.stream()
				.map(Measurement::getMeasurementName)
				.collect(toList());

			if (!mNames.contains(measurementName)) {
				continue;
			}
			// This run contains the deleted measurement => need to invalidate run instance in cache
			repoRunCache.invalidate(run.getCommitHash());
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
