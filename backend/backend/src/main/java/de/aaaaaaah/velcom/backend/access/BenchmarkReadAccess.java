package de.aaaaaaah.velcom.backend.access;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.Tables.MEASUREMENT;
import static org.jooq.codegen.db.Tables.MEASUREMENT_VALUE;
import static org.jooq.codegen.db.tables.Run.RUN;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.access.entities.benchmark.NewMeasurement;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.RepoReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.Repo;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.shared.util.Either;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jooq.codegen.db.tables.records.MeasurementRecord;
import org.jooq.codegen.db.tables.records.RunRecord;

// TODO: 27.11.20 Get rid of dimension caches
// TODO: 27.11.20 Insert or update dimensions before run (use transaction)

/**
 * Provides read access to benchmark related entities such as runs and measurements.
 */
public class BenchmarkReadAccess {

	protected static final int RECENT_RUN_CACHE_SIZE = 10;

	protected final DatabaseStorage databaseStorage;
	protected final RepoReadAccess repoAccess;

	protected final Map<RepoId, Cache<CommitHash, Run>> repoRunCache = new ConcurrentHashMap<>();
	protected final List<Run> recentRunCache = new ArrayList<>();
	protected final Comparator<Run> recentRunCacheOrder = comparing(Run::getStartTime).reversed();

	protected final Map<Dimension, DimensionInfo> dimensions = new ConcurrentHashMap<>();

	public BenchmarkReadAccess(DatabaseStorage databaseStorage, RepoReadAccess repoAccess) {
		this.databaseStorage = Objects.requireNonNull(databaseStorage);
		this.repoAccess = Objects.requireNonNull(repoAccess);

		loadDimensions();

		// Populate recent run cache
		reloadRecentRunCache();
	}

	/**
	 * Gets the most recent runs ordered by their start time.
	 *
	 * @param skip how many recent runs to skip
	 * @param amount how many recent runs to collect
	 * @return a sorted list containing the recent runs
	 * @throws IllegalArgumentException if skip/amount is negative
	 */
	public List<Run> getRecentRuns(int skip, int amount) throws IllegalArgumentException {
		if (skip < 0) {
			throw new IllegalArgumentException("skip must be positive");
		}
		if (amount < 0) {
			throw new IllegalArgumentException("amount must be positive");
		}
		if (amount == 0) {
			return Collections.emptyList();
		}

		List<Run> runList = new ArrayList<>();

		// Check cache
		checkCachesForDeletedRepos();

		synchronized (recentRunCache) {
			if (skip < recentRunCache.size()) {
				// There are at least some relevant runs in cache
				recentRunCache.stream().skip(skip).limit(amount).forEach(runList::add);
			}
		}

		// Check database
		if (skip >= recentRunCache.size() || skip + amount > recentRunCache.size()) {
			// Need to load even more runs

			int dbSkip = Math.max(skip, recentRunCache.size());
			int dbAmount = amount - runList.size();

			try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
				Map<String, RunRecord> runRecordMap = db.selectFrom(RUN)
					.orderBy(RUN.START_TIME.desc())
					.limit(dbSkip, dbAmount)
					.fetchMap(RUN.ID);

				runList.addAll(loadRunData(db, runRecordMap));
			}
		}

		runList.sort(recentRunCacheOrder);
		return runList;
	}

	/**
	 * Get a run by its id.
	 *
	 * @param id the run's id
	 * @return the run, if one exists
	 * @throws NoSuchRunException if no run with the given id is found
	 */
	public Run getRun(RunId id) throws NoSuchRunException {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			Map<String, RunRecord> runRecordMap = db.selectFrom(RUN)
				.where(RUN.ID.eq(id.getId().toString())).fetchMap(RUN.ID);

			if (runRecordMap.isEmpty()) {
				throw new NoSuchRunException(id);
			}

			return loadRunData(db, runRecordMap).get(0);
		}
	}

	/**
	 * Gets the latest run for the given commit.
	 *
	 * @param repoId the id of the repository the commit is in
	 * @param commitHash the hash of the commit
	 * @return the latest run for the commit, or {@link Optional#empty()} if no run for that commit
	 * 	exists yet.
	 */
	public Optional<Run> getLatestRun(RepoId repoId, CommitHash commitHash) {
		Map<CommitHash, Run> resultMap = getLatestRuns(repoId, List.of(commitHash));
		return resultMap.containsKey(commitHash) ? Optional.of(resultMap.get(commitHash))
			: Optional.empty();
	}

	/**
	 * Gets all runs for the given commit, ordered from latest to oldest.
	 *
	 * @param repoId the id of the repository the commit is in
	 * @param commitHash the hash of the commit
	 * @return all runs for the commit, ordered from latest to oldest.
	 */
	public List<Run> getAllRuns(RepoId repoId, CommitHash commitHash) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			Map<String, RunRecord> runRecordMap = db.selectFrom(RUN)
				.where(RUN.REPO_ID.eq(repoId.getId().toString()))
				.and(RUN.COMMIT_HASH.eq(commitHash.getHash()))
				.fetchMap(RUN.ID);

			return loadRunData(db, runRecordMap);
		}
	}

	/**
	 * Gets the latest runs for the given commits.
	 *
	 * @param repoId the id of the repository that the commits are from
	 * @param commitHashes the hashes of the commits
	 * @return a map that maps each commit hash to its latest run
	 */
	public Map<CommitHash, Run> getLatestRuns(RepoId repoId, Collection<CommitHash> commitHashes) {
		Objects.requireNonNull(repoId);
		Objects.requireNonNull(commitHashes);

		Map<CommitHash, Run> resultMap = new HashMap<>();

		// Check cache
		checkCachesForDeletedRepos();

		final Cache<CommitHash, Run> repoRunCache = this.repoRunCache.computeIfAbsent(repoId,
			r -> buildRunCache(repoId)
		);

		repoRunCache.getAllPresent(commitHashes).forEach(resultMap::put);

		// Check database
		Set<String> uncachedCommitHashes = commitHashes.stream()
			.filter(hash -> !resultMap.containsKey(hash))
			.map(CommitHash::getHash)
			.collect(toSet());

		if (!uncachedCommitHashes.isEmpty()) {
			try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
				// Get all data from database
				Map<String, RunRecord> runRecordMap = db.selectFrom(RUN)
					.where(RUN.REPO_ID.eq(repoId.getId().toString()))
					.and(RUN.COMMIT_HASH.in(uncachedCommitHashes))
					.fetchMap(RUN.ID);

				loadRunData(db, runRecordMap).forEach(run -> {
					CommitHash hash = run.getSource().getLeft().orElseThrow().getHash();
					resultMap.put(hash, run);

					// Insert run into cache
					repoRunCache.put(hash, run);
				});
			}
		}

		return resultMap;
	}

	@Timed(histogram = true)
	private List<Run> loadRunData(DBReadAccess db, Map<String, RunRecord> runRecordMap) {
		if (runRecordMap.isEmpty()) {
			return Collections.emptyList();
		}

		// 1.) Load measurements from database
		Map<String, MeasurementRecord> measurementRecordMap = db.selectFrom(MEASUREMENT)
			.where(MEASUREMENT.RUN_ID.in(runRecordMap.keySet()))
			.fetchMap(MEASUREMENT.ID);

		// 2.) Load measurement values from database
		Map<String, List<Double>> valueMap = db.selectFrom(MEASUREMENT_VALUE)
			.where(MEASUREMENT_VALUE.MEASUREMENT_ID.in(measurementRecordMap.keySet()))
			.fetchGroups(MEASUREMENT_VALUE.MEASUREMENT_ID, MEASUREMENT_VALUE.VALUE);

		// 3.) Create measurement entities
		Map<RunId, List<Measurement>> runToMeasurementMap = new HashMap<>();

		measurementRecordMap.values().forEach(measurementRecord -> {
			// Read basic measurement data
			RunRecord runRecord = runRecordMap.get(measurementRecord.getRunId());
			RunId runId = new RunId(UUID.fromString(runRecord.getId()));
			Dimension dimension = new Dimension(
				measurementRecord.getBenchmark(), measurementRecord.getMetric()
			);

			final Measurement measurement;

			// Read measurement content
			if (measurementRecord.getError() != null) {
				var measurementError = new MeasurementError(measurementRecord.getError());
				measurement = new Measurement(runId, dimension, measurementError);
			} else {
				List<Double> values = valueMap.get(measurementRecord.getId());
				var measurementValues = new MeasurementValues(values);
				measurement = new Measurement(runId, dimension, measurementValues);
			}

			// Insert measurement into map
			if (!runToMeasurementMap.containsKey(runId)) {
				runToMeasurementMap.put(runId, new ArrayList<>());
			}

			runToMeasurementMap.get(runId).add(measurement);
		});

		// 4.) Create run entities
		return runRecordMap.values().stream()
			.map(runRecord -> {
				RunId runId = new RunId(UUID.fromString(runRecord.getId()));

				final Either<CommitSource, TarSource> source;

				if (runRecord.getCommitHash() != null) {
					source = Either.ofLeft(new CommitSource(
						new RepoId(UUID.fromString(runRecord.getRepoId())),
						new CommitHash(runRecord.getCommitHash())
					));
				} else {
					// If commit hash is null, tar desc must be present
					source = Either.ofRight(new TarSource(runRecord.getTarDesc()));
				}

				if (runRecord.getError() != null) {
					RunError error = new RunError(
						runRecord.getError(),
						RunErrorType.fromTextualRepresentation(runRecord.getErrorType())
					);

					return new Run(
						runId,
						runRecord.getAuthor(),
						runRecord.getRunnerName(),
						runRecord.getRunnerInfo(),
						runRecord.getStartTime(

						),
						runRecord.getStopTime(),
						source,
						error
					);
				} else {
					List<Measurement> measurements = runToMeasurementMap.getOrDefault(runId, List.of());

					return new Run(
						runId,
						runRecord.getAuthor(),
						runRecord.getRunnerName(),
						runRecord.getRunnerInfo(),
						runRecord.getStartTime(),
						runRecord.getStopTime(),
						source,
						measurements
					);
				}
			})
			.collect(toList());
	}

	protected void reloadRecentRunCache() {
		synchronized (this.recentRunCache) {
			this.recentRunCache.clear();
			this.recentRunCache.addAll(getRecentRuns(0, RECENT_RUN_CACHE_SIZE));
			this.recentRunCache.sort(recentRunCacheOrder);
		}
	}

	protected void checkCachesForDeletedRepos() {
		// Don't cache data for repos that don't exist any more
		Set<RepoId> ids = repoAccess.getAllRepos().stream()
			.map(Repo::getId)
			.collect(Collectors.toSet());
		repoRunCache.keySet().retainAll(ids);

		synchronized (recentRunCache) {
			boolean recentRunRepoWasDeleted = recentRunCache.stream()
				.map(Run::getRepoId)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.anyMatch(repoId -> !ids.contains(repoId));

			if (recentRunRepoWasDeleted) {
				reloadRecentRunCache(); // recentRunCache is now invalid
			}
		}
	}

	/**
	 * Load all dimensions and their infos from the db.
	 */
	private void loadDimensions() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {

			// Figure out which dimensions exist at all
			db.selectDistinct(MEASUREMENT.BENCHMARK, MEASUREMENT.METRIC)
				.from(MEASUREMENT)
				.forEach(record -> {
					Dimension dimension = new Dimension(record.component1(), record.component2());
					dimensions.put(dimension, new DimensionInfo(dimension));
				});

			// Figure out the latest units
			db.selectDistinct(MEASUREMENT.BENCHMARK, MEASUREMENT.METRIC, MEASUREMENT.UNIT)
				.from(RUN)
				.join(MEASUREMENT).on(MEASUREMENT.RUN_ID.eq(RUN.ID))
				.where(MEASUREMENT.UNIT.isNotNull())
				.groupBy(MEASUREMENT.BENCHMARK, MEASUREMENT.METRIC)
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
			db.selectDistinct(MEASUREMENT.BENCHMARK, MEASUREMENT.METRIC, MEASUREMENT.INTERPRETATION)
				.from(RUN)
				.join(MEASUREMENT).on(MEASUREMENT.RUN_ID.eq(RUN.ID))
				.where(MEASUREMENT.INTERPRETATION.isNotNull())
				.groupBy(MEASUREMENT.BENCHMARK, MEASUREMENT.METRIC)
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
	}

	/**
	 * Update the measurement's dimension's info if necessary.
	 *
	 * @param measurement the measurement whose unit and interpretation to use for the update
	 */
	protected void updateDimensions(NewMeasurement measurement) {
		if (measurement.getUnit().isPresent() || measurement.getInterpretation().isPresent()) {
			dimensions.compute(measurement.getDimension(), (dimension, info) -> {
				if (info == null) {
					DimensionInfo defaultInfo = new DimensionInfo(dimension);
					return new DimensionInfo(
						dimension,
						measurement.getUnit().orElse(defaultInfo.getUnit()),
						measurement.getInterpretation().orElse(defaultInfo.getInterpretation()),
						defaultInfo.isSignificant()
					);
				} else {
					return new DimensionInfo(
						dimension,
						measurement.getUnit().orElse(info.getUnit()),
						measurement.getInterpretation().orElse(info.getInterpretation()),
						info.isSignificant()
					);
				}
			});
		}
	}

	protected static Cache<CommitHash, Run> buildRunCache(RepoId repoId) {
		final Cache<CommitHash, Run> cache = Caffeine.newBuilder()
			.recordStats()
			.maximumSize(10000)
			.build();

		CaffeineCacheMetrics.monitor(
			Metrics.globalRegistry,
			cache,
			"repoRunCache",
			"repo",
			repoId.getId().toString()
		);

		return cache;
	}

}
