package de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.jooq.codegen.db.Tables.DIMENSION;
import static org.jooq.codegen.db.tables.Task.TASK;

import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.builder.NewMeasurement;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.builder.NewRun;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.caches.AvailableDimensionsCache;
import de.aaaaaaah.velcom.backend.newaccess.caches.LatestRunCache;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.codegen.db.tables.records.DimensionRecord;
import org.jooq.codegen.db.tables.records.MeasurementRecord;
import org.jooq.codegen.db.tables.records.MeasurementValueRecord;
import org.jooq.codegen.db.tables.records.RunRecord;

public class BenchmarkWriteAccess extends BenchmarkReadAccess {

	private final AvailableDimensionsCache availableDimensionsCache;
	private final LatestRunCache latestRunCache;

	public BenchmarkWriteAccess(DatabaseStorage databaseStorage,
		AvailableDimensionsCache availableDimensionsCache, LatestRunCache latestRunCache) {

		super(databaseStorage);

		this.availableDimensionsCache = availableDimensionsCache;
		this.latestRunCache = latestRunCache;
	}

	/**
	 * Inserts the specified run into the database. Also deletes the task with the same id from the	 *
	 * task table in the same transaction.
	 *
	 * @param newRun the run to insert
	 */
	public void insertRun(NewRun newRun) {
		databaseStorage.acquireWriteTransaction(db -> {
			deleteTask(db, newRun.getId().toTaskId());
			updateDimensions(db, newRun);
			insertNewRun(db, newRun);
			insertNewMeasurements(db, newRun);
		});

		newRun.getRepoId().ifPresent(availableDimensionsCache::invalidate);
		newRun.getSource().getLeft().ifPresent(
			commitSource -> latestRunCache.invalidate(commitSource.getRepoId(), commitSource.getHash()));
	}

	private void deleteTask(DBWriteAccess db, TaskId taskId) {
		db.deleteFrom(TASK)
			.where(TASK.ID.eq(taskId.getIdAsString()))
			.execute();
	}

	private void updateDimensions(DBWriteAccess db, NewRun newRun) {
		Map<Dimension, DimensionRecord> dimensions = db.selectFrom(DIMENSION)
			.stream()
			.collect(toMap(
				record -> new Dimension(record.getBenchmark(), record.getMetric()),
				it -> it
			));

		newRun.getResult()
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
}
