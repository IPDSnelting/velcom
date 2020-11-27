package de.aaaaaaah.velcom.backend.newaccess.dimensionaccess;

import static org.jooq.codegen.db.tables.Dimension.DIMENSION;
import static org.jooq.codegen.db.tables.Measurement.MEASUREMENT;
import static org.jooq.codegen.db.tables.Run.RUN;

import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DBWriteAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jooq.codegen.db.Tables;
import org.jooq.codegen.db.tables.records.DimensionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DimensionReadAccess {

	private final Logger LOGGER = LoggerFactory.getLogger(DimensionReadAccess.class);

	protected final DatabaseStorage databaseStorage;

	public DimensionReadAccess(DatabaseStorage databaseStorage) {
		this.databaseStorage = databaseStorage;
	}

	private static Dimension dimRecordToDim(DimensionRecord record) {
		return new Dimension(record.getBenchmark(), record.getMetric());
	}

	private static DimensionInfo dimRecordToDimInfo(DimensionRecord record) {
		return new DimensionInfo(
			dimRecordToDim(record),
			new Unit(record.getUnit()),
			Interpretation.fromTextualRepresentation(record.getInterpretation()),
			record.getSignificant()
		);
	}

	private static DimensionRecord dimInfoToDimRecord(DimensionInfo info) {
		return new DimensionRecord(
			info.getDimension().getBenchmark(),
			info.getDimension().getMetric(),
			info.getUnit().getName(),
			info.getInterpretation().getTextualRepresentation(),
			info.isSignificant()
		);
	}

	/**
	 * Find a repo's available dimensions, i. e. the dimensions a repo has at least one measurement
	 * for.
	 *
	 * @param repoId the repo's id
	 * @return the repo's available dimensions
	 */
	public Set<Dimension> getAvailableDimensions(RepoId repoId) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectDistinct(MEASUREMENT.BENCHMARK, MEASUREMENT.METRIC)
				.from(RUN)
				.join(MEASUREMENT).on(MEASUREMENT.RUN_ID.eq(RUN.ID))
				.where(RUN.REPO_ID.eq(repoId.getIdAsString()))
				.and(RUN.COMMIT_HASH.isNotNull())
				.stream()
				.map(record -> new Dimension(record.value1(), record.value2()))
				.collect(Collectors.toSet());
		}
	}

	/**
	 * Find multiple repos' available dimensions, i. e. the dimensions a repo has at least one
	 * measurement for.
	 *
	 * @param repoIds the ids of the repos to return the available dimensions of
	 * @return the available dimensions for each input repo, including those that don't have any
	 * 	available dimensions. In other words, this is guaranteed to contain an entry for every input
	 * 	repo id.
	 */
	public Map<RepoId, Set<Dimension>> getAvailableDimensions(Collection<RepoId> repoIds) {
		Set<String> repoIdStrings = repoIds.stream()
			.map(RepoId::getIdAsString)
			.collect(Collectors.toSet());

		final HashMap<RepoId, Set<Dimension>> availableDimensions;

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			availableDimensions = db
				.selectDistinct(RUN.REPO_ID, MEASUREMENT.BENCHMARK, MEASUREMENT.METRIC)
				.from(RUN)
				.join(MEASUREMENT).on(MEASUREMENT.RUN_ID.eq(RUN.ID))
				.where(RUN.REPO_ID.in(repoIdStrings))
				.and(RUN.COMMIT_HASH.isNotNull())
				.stream()
				.collect(Collectors.groupingBy(
					record -> RepoId.fromString(record.value1()),
					HashMap::new,
					Collectors.mapping(
						record -> new Dimension(record.value2(), record.value3()),
						Collectors.toSet()
					)
				));
		}

		for (RepoId repoId : repoIds) {
			availableDimensions.putIfAbsent(repoId, Set.of());
		}

		return availableDimensions;
	}

	///////////////
	// Migration //
	///////////////

	// TODO: 25.11.20 Remove after migration
	public void migrate() {
		if (!needToMigrate()) {
			LOGGER.debug("No need to migrate dimensions");
			return;
		}

		LOGGER.info("Migrating dimensions");
		insertAllDimensions(findAllCurrentDimensions());
	}

	// TODO: 25.11.20 Remove after migration
	private boolean needToMigrate() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectFrom(DIMENSION)
				.fetchOptional()
				.isEmpty();
		}
	}

	// TODO: 25.11.20 Remove after migration
	private Set<DimensionInfo> findAllCurrentDimensions() {
		Map<Dimension, DimensionInfo> dimensions = new HashMap<>();

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {

			// Figure out which dimensions exist at all
			db.selectDistinct(Tables.MEASUREMENT.BENCHMARK, Tables.MEASUREMENT.METRIC)
				.from(Tables.MEASUREMENT)
				.forEach(record -> {
					Dimension dimension = new Dimension(record.component1(), record.component2());
					dimensions.put(dimension, new DimensionInfo(dimension));
				});

			// Figure out the latest units
			db.selectDistinct(Tables.MEASUREMENT.BENCHMARK, Tables.MEASUREMENT.METRIC,
				Tables.MEASUREMENT.UNIT)
				.from(RUN)
				.join(Tables.MEASUREMENT).on(Tables.MEASUREMENT.RUN_ID.eq(RUN.ID))
				.where(Tables.MEASUREMENT.UNIT.isNotNull())
				.groupBy(Tables.MEASUREMENT.BENCHMARK, Tables.MEASUREMENT.METRIC)
				.orderBy(RUN.STOP_TIME.desc())
				.forEach(record -> {
					Dimension dimension = new Dimension(record.value1(), record.value2());
					DimensionInfo info = dimensions.get(dimension);
					DimensionInfo newInfo = new DimensionInfo(
						info.getDimension(),
						new Unit(record.value3()),
						info.getInterpretation(),
						info.isSignificant()
					);
					dimensions.put(dimension, newInfo);
				});

			// Figure out the latest interpretations
			db.selectDistinct(Tables.MEASUREMENT.BENCHMARK, Tables.MEASUREMENT.METRIC,
				Tables.MEASUREMENT.INTERPRETATION)
				.from(RUN)
				.join(Tables.MEASUREMENT).on(Tables.MEASUREMENT.RUN_ID.eq(RUN.ID))
				.where(Tables.MEASUREMENT.INTERPRETATION.isNotNull())
				.groupBy(Tables.MEASUREMENT.BENCHMARK, Tables.MEASUREMENT.METRIC)
				.orderBy(RUN.STOP_TIME.desc())
				.forEach(record -> {
					Dimension dimension = new Dimension(record.value1(), record.value2());
					DimensionInfo info = dimensions.get(dimension);
					DimensionInfo newInfo = new DimensionInfo(
						info.getDimension(),
						info.getUnit(),
						Interpretation.fromTextualRepresentation(record.value3()),
						info.isSignificant()
					);
					dimensions.put(dimension, newInfo);
				});
		}

		return new HashSet<>(dimensions.values());
	}

	// TODO: 25.11.20 Remove after migration
	private void insertAllDimensions(Set<DimensionInfo> dimensions) {
		List<DimensionRecord> dimRecords = dimensions.stream()
			.map(DimensionReadAccess::dimInfoToDimRecord)
			.collect(Collectors.toList());

		try (DBWriteAccess db = databaseStorage.acquireWriteAccess()) {
			db.dsl().batchInsert(dimRecords).execute();
		}
	}
}
