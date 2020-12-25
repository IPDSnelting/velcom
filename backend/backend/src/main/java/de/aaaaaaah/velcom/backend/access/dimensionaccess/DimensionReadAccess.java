package de.aaaaaaah.velcom.backend.access.dimensionaccess;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.tables.Dimension.DIMENSION;
import static org.jooq.codegen.db.tables.Measurement.MEASUREMENT;
import static org.jooq.codegen.db.tables.Run.RUN;

import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.exceptions.NoSuchDimensionException;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jooq.codegen.db.tables.records.DimensionRecord;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides read access to the global list of dimensions and their associated information.
 */
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

	/**
	 * Convert a {@link DimensionInfo} to a JDBC record for the "dimension" table.
	 *
	 * @param info the dimension info to convert
	 * @return the newly created database record
	 */
	public static DimensionRecord dimInfoToDimRecord(DimensionInfo info) {
		return new DimensionRecord(
			info.getDimension().getBenchmark(),
			info.getDimension().getMetric(),
			info.getUnit().getName(),
			info.getInterpretation().getTextualRepresentation(),
			info.isSignificant()
		);
	}

	/**
	 * Check if a dimension exists.
	 *
	 * @param dimension the dimension whose existence to check
	 * @throws NoSuchDimensionException if the dimension doesn't exist
	 */
	public void guardDimensionExists(Dimension dimension) throws NoSuchDimensionException {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			db.selectFrom(DIMENSION)
				.where(DIMENSION.BENCHMARK.eq(dimension.getBenchmark()))
				.and(DIMENSION.METRIC.eq(dimension.getMetric()))
				.fetchSingle();
		} catch (DataAccessException e) {
			throw new NoSuchDimensionException(e, dimension);
		}
	}

	/**
	 * Get a dimension's info. Always returns a {@link DimensionInfo}, even if the dimension does not
	 * exist (in which case it uses the default values).
	 *
	 * @param dimension the dimension whose info to get
	 * @return that dimension's info (or the default info if the dimension doesn't exist)
	 */
	public DimensionInfo getDimensionInfo(Dimension dimension) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectFrom(DIMENSION)
				.where(DIMENSION.BENCHMARK.eq(dimension.getBenchmark()))
				.and(DIMENSION.METRIC.eq(dimension.getMetric()))
				.fetchOptional()
				.map(DimensionReadAccess::dimRecordToDimInfo)
				.orElse(new DimensionInfo(dimension));
		}
	}

	/**
	 * Get {@link DimensionInfo}s for multiple dimensions at the same time. Similar to {@link
	 * #getDimensionInfo(Dimension)}, this function will return a {@link DimensionInfo} for every
	 * dimension even if it doesn't exist.
	 *
	 * @param dimensions the dimensions whose infos to get
	 * @return an info for every input dimension. This set should always be the same size as the input
	 * 	set.
	 */
	public Set<DimensionInfo> getDimensionInfos(Set<Dimension> dimensions) {
		final Set<DimensionInfo> infos;

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			infos = db.selectFrom(DIMENSION)
				.stream() // This is efficient enough, we don't have that many dimensions
				.map(DimensionReadAccess::dimRecordToDimInfo)
				.filter(info -> dimensions.contains(info.getDimension()))
				.collect(Collectors.toCollection(HashSet::new));
		}

		dimensions.forEach(dimension -> infos.add(new DimensionInfo(dimension)));

		return infos;
	}

	/**
	 * Get {@link DimensionInfo}s for multiple dimensions at the same time. Works the same as {@link
	 * #getDimensionInfos(Set)} but sorts the dimensions into a map for convenience.
	 *
	 * @param dimensions the dimensions whose infos to get
	 * @return an info for every input dimension. This map should always be the same size as the input
	 * 	set.
	 */
	public Map<Dimension, DimensionInfo> getDimensionInfoMap(Set<Dimension> dimensions) {
		return getDimensionInfos(dimensions).stream()
			.collect(toMap(DimensionInfo::getDimension, it -> it));
	}

	/**
	 * @return all currently known dimensions
	 */
	public Set<DimensionInfo> getAllDimensions() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectFrom(DIMENSION)
				.stream()
				.map(DimensionReadAccess::dimRecordToDimInfo)
				.collect(toSet());
		}
	}

	public Set<Dimension> getSignificantDimensions() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectFrom(DIMENSION)
				.where(DIMENSION.SIGNIFICANT)
				.stream()
				.map(DimensionReadAccess::dimRecordToDim)
				.collect(toSet());
		}
	}

	/**
	 * Find a repo's available dimensions, i. e. the dimensions a repo has at least one measurement
	 * for. Only considers runs for commits, not runs for tars.
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
				.collect(toSet());
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
			.collect(toSet());

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
						toSet()
					)
				));
		}

		for (RepoId repoId : repoIds) {
			availableDimensions.putIfAbsent(repoId, Set.of());
		}

		return availableDimensions;
	}
}
