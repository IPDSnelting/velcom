package de.aaaaaaah.velcom.backend.newaccess;

import static org.jooq.codegen.db.Tables.RUN_MEASUREMENT_VALUE;
import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.codegen.db.tables.RunMeasurement.RUN_MEASUREMENT;
import static org.jooq.impl.DSL.exists;

import com.github.benmanes.caffeine.cache.Cache;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.newaccess.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.entities.Run;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.codegen.db.tables.records.RunMeasurementRecord;
import org.jooq.codegen.db.tables.records.RunRecord;

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
		try (DSLContext db = databaseStorage.acquireContext()) {
			// 1.) Insert run into database
			RunRecord runRecord = db.newRecord(RUN);
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

					RunMeasurementRecord measurementRecord = db.newRecord(RUN_MEASUREMENT);
					measurementRecord.setId(measurementId);
					measurementRecord.setRunId(run.getId().getId().toString());
					measurementRecord.setBenchmark(measurement.getMeasurementName().getBenchmark());
					measurementRecord.setMetric(measurement.getMeasurementName().getMetric());

					if (measurement.getContent().isLeft()) {
						MeasurementError error = measurement.getContent().getLeft().orElseThrow();

						measurementRecord.setErrorMessage(error.getErrorMessage());
						measurementRecord.insert();
					} else {
						MeasurementValues values = measurement.getContent().getRight()
							.orElseThrow();

						measurementRecord.setUnit(values.getUnit().getName());
						measurementRecord.setInterpretation(
							values.getInterpretation().getTextualRepresentation()
						);
						measurementRecord.insert();

						// 2.1.) Insert values into database
						var valuesInsertStep = db.insertInto(RUN_MEASUREMENT_VALUE)
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

			// 3.) Insert run into cache
			recentRunCache.addFirst(run);
			while (recentRunCache.size() > RECENT_RUN_CACHE_SIZE) {
				recentRunCache.removeLast();
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
	}

}
