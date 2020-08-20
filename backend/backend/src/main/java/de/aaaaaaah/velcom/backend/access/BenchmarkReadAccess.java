package de.aaaaaaah.velcom.backend.access;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.Tables.MEASUREMENT;
import static org.jooq.codegen.db.Tables.MEASUREMENT_VALUE;
import static org.jooq.codegen.db.tables.Run.RUN;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.access.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunError;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.access.entities.Unit;
import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.codegen.db.tables.records.MeasurementRecord;
import org.jooq.codegen.db.tables.records.RunRecord;
import org.jooq.impl.DSL;

/**
 * Provides read access to benchmark related entities such as runs and measurements.
 */
public class BenchmarkReadAccess {

	protected static final Caffeine<Object, Object> RUN_CACHE_BUILDER = Caffeine.newBuilder()
		.maximumSize(10000);
	protected static final int RECENT_RUN_CACHE_SIZE = 10;

	protected final DatabaseStorage databaseStorage;
	protected final RepoReadAccess repoAccess;

	protected final Map<RepoId, Cache<CommitHash, Run>> repoRunCache = new ConcurrentHashMap<>();
	protected final List<Run> recentRunCache = new ArrayList<>();
	protected final Comparator<Run> recentRunCacheOrder = comparing(Run::getStartTime).reversed();
	protected final Map<RepoId, Set<Dimension>> dimensionCache = new ConcurrentHashMap<>();

	public BenchmarkReadAccess(DatabaseStorage databaseStorage, RepoReadAccess repoAccess) {
		this.databaseStorage = Objects.requireNonNull(databaseStorage);
		this.repoAccess = Objects.requireNonNull(repoAccess);

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

			try (DSLContext db = databaseStorage.acquireContext()) {
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
		try (DSLContext db = databaseStorage.acquireContext()) {
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
		try (DSLContext db = databaseStorage.acquireContext()) {
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
			r -> RUN_CACHE_BUILDER.build()
		);

		repoRunCache.getAllPresent(commitHashes).forEach(resultMap::put);

		// Check database
		Set<String> uncachedCommitHashes = commitHashes.stream()
			.filter(hash -> !resultMap.containsKey(hash))
			.map(CommitHash::getHash)
			.collect(toSet());

		if (!uncachedCommitHashes.isEmpty()) {
			try (DSLContext db = databaseStorage.acquireContext()) {
				// Get all data from database
				Map<String, RunRecord> runRecordMap = db.selectFrom(RUN)
					.where(RUN.REPO_ID.eq(repoId.getId().toString()))
					.and(RUN.COMMIT_HASH.in(uncachedCommitHashes))
					.fetchMap(RUN.ID);

				loadRunData(db, runRecordMap).forEach(run -> {
					CommitHash hash = run.getSource().orElseThrow().getHash();
					resultMap.put(hash, run);

					// Insert run into cache
					repoRunCache.put(hash, run);
				});
			}
		}

		return resultMap;
	}

	/**
	 * Gets all available dimensions for the specified repositories.
	 *
	 * @param repoIds the ids of the repositories
	 * @return a map with each repo id as a key mapping to the set of available dimensions for the
	 * 	given repository
	 */
	public Map<RepoId, Set<Dimension>> getAvailableDimensions(List<RepoId> repoIds) {
		List<String> repoIdStrList = repoIds.stream()
			.map(repoId -> repoId.getId().toString())
			.collect(Collectors.toCollection(ArrayList::new));

		Map<RepoId, Set<Dimension>> resultMap = new HashMap<>();

		// 1.) Check cache
		checkCachesForDeletedRepos();

		for (RepoId repoId : repoIds) {
			Set<Dimension> dimensions = dimensionCache.get(repoId);

			if (dimensions != null) {
				resultMap.put(repoId, Collections.unmodifiableSet(dimensions));
				repoIdStrList.remove(repoId.getId().toString());
			} else {
				resultMap.put(repoId, new HashSet<>()); // prepare for database query
			}
		}

		// 2.) Check database
		if (!repoIdStrList.isEmpty()) {
			try (DSLContext db = databaseStorage.acquireContext()) {
				// SQLite guarantees that we get the correct row (correct interp and unit)
				// in each group. (See https://sqlite.org/lang_select.html#bareagg)
				db.select(RUN.REPO_ID, MEASUREMENT.BENCHMARK, MEASUREMENT.METRIC,
					MEASUREMENT.UNIT, MEASUREMENT.INTERPRETATION, DSL.max(RUN.STOP_TIME))
					.from(MEASUREMENT)
					.join(RUN).on(RUN.ID.eq(MEASUREMENT.RUN_ID))
					.groupBy(RUN.REPO_ID, MEASUREMENT.BENCHMARK, MEASUREMENT.METRIC)
					.forEach(record -> {
						RepoId repoId = new RepoId(UUID.fromString(record.value1()));
						MeasurementName name = new MeasurementName(
							record.value2(),
							record.value3()
						);
						Unit unit = new Unit(record.value4());
						Interpretation interp = Interpretation.fromTextualRepresentation(record.value5());
						Dimension dimension = new Dimension(name, unit, interp);

						resultMap.get(repoId).add(dimension);
					});
			}

			// 3.) Update cache with data collected from database
			for (String repoIdStr : repoIdStrList) { // <- all repos in this list are not in cache
				RepoId repoId = new RepoId(UUID.fromString(repoIdStr));
				// Create copy so that no mutation can occur from outside
				Set<Dimension> dimensionSet = new HashSet<>(resultMap.get(repoId));
				dimensionCache.put(repoId, dimensionSet);
			}
		}

		return resultMap;
	}

	private List<Run> loadRunData(DSLContext db, Map<String, RunRecord> runRecordMap) {
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
			MeasurementName measurementName = new MeasurementName(
				measurementRecord.getBenchmark(), measurementRecord.getMetric()
			);
			Unit unit = new Unit(
				measurementRecord.getUnit() == null ? "" : measurementRecord.getUnit()
			);
			Interpretation interpretation = measurementRecord.getInterpretation() == null
				? Interpretation.NEUTRAL
				: Interpretation.fromTextualRepresentation(measurementRecord.getInterpretation());

			final Measurement measurement;

			// Read measurement content
			if (measurementRecord.getError() != null) {
				var measurementError = new MeasurementError(measurementRecord.getError());

				measurement = new Measurement(
					runId, measurementName, unit, interpretation, measurementError
				);
			} else {
				List<Double> values = valueMap.get(measurementRecord.getId());
				var measurementValues = new MeasurementValues(values);

				measurement = new Measurement(
					runId, measurementName, unit, interpretation, measurementValues
				);
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

				CommitSource nullableSource = null;

				if (runRecord.getRepoId() != null) {
					nullableSource = new CommitSource(
						new RepoId(UUID.fromString(runRecord.getRepoId())),
						new CommitHash(runRecord.getCommitHash())
					);
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
						runRecord.getStartTime().toInstant(),
						runRecord.getStopTime().toInstant(),
						nullableSource,
						error
					);
				} else {
					List<Measurement> measurements = runToMeasurementMap.get(runId);

					return new Run(
						runId,
						runRecord.getAuthor(),
						runRecord.getRunnerName(),
						runRecord.getRunnerInfo(),
						runRecord.getStartTime().toInstant(),
						runRecord.getStopTime().toInstant(),
						nullableSource,
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
		// Get list of repos that currently exist and check if we cache data for repos that
		// dont exist any more
		Collection<RepoId> ids = repoAccess.getAllRepoIds();

		repoRunCache.keySet().removeIf(repoId -> !ids.contains(repoId));
		dimensionCache.keySet().removeIf(repoId -> !ids.contains(repoId));

		synchronized (recentRunCache) {
			boolean recentRunRepoWasDeleted = recentRunCache.stream()
				.flatMap(run -> run.getSource().stream())
				.anyMatch(source -> !ids.contains(source.getRepoId()));

			if (recentRunRepoWasDeleted) {
				reloadRecentRunCache(); // recentRunCache is now invalid
			}
		}
	}

}
