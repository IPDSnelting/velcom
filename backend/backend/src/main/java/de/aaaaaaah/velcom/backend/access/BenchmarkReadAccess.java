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
import de.aaaaaaah.velcom.backend.newaccess.AccessUtils;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
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

			// Read measurement content
			final Either<MeasurementError, MeasurementValues> content;
			if (measurementRecord.getError() != null) {
				content = Either.ofLeft(new MeasurementError(measurementRecord.getError()));
			} else {
				content = Either.ofRight(new MeasurementValues(valueMap.get(measurementRecord.getId())));
			}

			Measurement measurement = new Measurement(runId, dimension, content);

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

				final Either<CommitSource, TarSource> source = AccessUtils.readSource(
					runRecord.getRepoId(),
					runRecord.getCommitHash(),
					runRecord.getTarDesc()
				);

				final Either<RunError, Collection<Measurement>> result;
				if (runRecord.getError() != null) {
					result = Either.ofLeft(new RunError(
						runRecord.getError(),
						RunErrorType.fromTextualRepresentation(runRecord.getErrorType())
					));
				} else {
					result = Either.ofRight(runToMeasurementMap.getOrDefault(runId, List.of()));
				}

				return new Run(
					runId,
					runRecord.getAuthor(),
					runRecord.getRunnerName(),
					runRecord.getRunnerInfo(),
					runRecord.getStartTime(),
					runRecord.getStopTime(),
					source,
					result
				);
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
