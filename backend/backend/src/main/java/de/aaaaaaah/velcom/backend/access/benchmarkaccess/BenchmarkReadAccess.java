package de.aaaaaaah.velcom.backend.access.benchmarkaccess;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.Tables.KNOWN_COMMIT;
import static org.jooq.codegen.db.Tables.LATEST_RUN;
import static org.jooq.codegen.db.Tables.MEASUREMENT;
import static org.jooq.codegen.db.Tables.MEASUREMENT_VALUE;
import static org.jooq.codegen.db.Tables.RUN;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.length;
import static org.jooq.impl.DSL.not;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.sum;

import de.aaaaaaah.velcom.backend.access.AccessUtils;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.SearchRunDescription;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.ShortRunDescription;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.shared.util.Either;
import de.aaaaaaah.velcom.shared.util.Pair;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.codegen.db.tables.records.RunRecord;

/**
 * Access for retrieving runs and their measurements.
 */
public class BenchmarkReadAccess {


	protected final DatabaseStorage databaseStorage;

	public BenchmarkReadAccess(DatabaseStorage databaseStorage) {
		this.databaseStorage = databaseStorage;
	}

	private List<Run> loadRuns(DBReadAccess db, List<RunRecord> runRecords) {
		if (runRecords.isEmpty()) {
			return List.of();
		}

		// 0. Preparation
		Set<String> runIds = runRecords.stream()
			.map(RunRecord::getId)
			.collect(toSet());

		// 1. Load measurement values (key: measurement id)
		Map<String, List<Double>> values = db.dsl()
			.select(MEASUREMENT.ID, MEASUREMENT_VALUE.VALUE)
			.from(MEASUREMENT)
			.join(MEASUREMENT_VALUE)
			.on(MEASUREMENT_VALUE.MEASUREMENT_ID.eq(MEASUREMENT.ID))
			.where(MEASUREMENT.RUN_ID.in(runIds))
			.fetchGroups(MEASUREMENT.ID, MEASUREMENT_VALUE.VALUE);

		// 2. Load measurements
		Map<RunId, List<Measurement>> measurements = db.dsl()
			.selectFrom(MEASUREMENT)
			.where(MEASUREMENT.RUN_ID.in(runIds))
			.stream()
			.map(record -> {
				final Either<MeasurementError, MeasurementValues> content;
				if (record.getError() != null) {
					content = Either.ofLeft(new MeasurementError(record.getError()));
				} else {
					content = Either.ofRight(new MeasurementValues(values.get(record.getId())));
				}

				return new Measurement(
					RunId.fromString(record.getRunId()),
					new Dimension(record.getBenchmark(), record.getMetric()),
					content
				);
			})
			.collect(groupingBy(Measurement::getRunId));

		// 3. Create run entities
		return runRecords.stream()
			.map(record -> {
				RunId runId = RunId.fromString(record.getId());

				final Either<CommitSource, TarSource> source = AccessUtils.readSource(
					record.getRepoId(),
					record.getCommitHash(),
					record.getTarDesc()
				);

				final Either<RunError, Collection<Measurement>> result;
				if (record.getError() != null) {
					result = Either.ofLeft(new RunError(
						record.getError(),
						RunErrorType.fromTextualRepresentation(record.getErrorType())
					));
				} else {
					result = Either.ofRight(measurements.getOrDefault(runId, List.of()));
				}

				return new Run(
					runId,
					record.getAuthor(),
					record.getRunnerName(),
					record.getRunnerInfo(),
					record.getStartTime(),
					record.getStopTime(),
					source,
					result
				);
			})
			.collect(toList());
	}

	/**
	 * Get a run by its id.
	 *
	 * @param runId the run's id
	 * @return the run, if one exists
	 * @throws NoSuchRunException if no run with the given id is found
	 */
	public Run getRun(RunId runId) throws NoSuchRunException {
		List<Run> runs = getRuns(List.of(runId));
		if (runs.size() == 1) {
			return runs.get(0);
		} else {
			throw new NoSuchRunException(runId);
		}
	}

	/**
	 * Get multiple runs by their ids.
	 *
	 * @param runIds the ids of the runs to retrieve
	 * @return a list containing only those of the runs that exist, in no particular order
	 */
	public List<Run> getRuns(Collection<RunId> runIds) {
		Set<String> runIdStrings = runIds.stream()
			.map(RunId::getIdAsString)
			.collect(toSet());

		return databaseStorage.acquireReadTransaction(db -> {
			List<RunRecord> runRecords = db.dsl()
				.selectFrom(RUN)
				.where(RUN.ID.in(runIdStrings))
				.stream()
				.collect(toList());

			return loadRuns(db, runRecords);
		});
	}

	/**
	 * Get all runs of a specific commit ordered by their start time.
	 *
	 * @param repoId the id of the repository the commit is in
	 * @param commitHash the hash of the commit
	 * @return all runs for the commit, ordered from new to old
	 */
	public List<RunId> getAllRunIds(RepoId repoId, CommitHash commitHash) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.selectFrom(RUN)
				.where(RUN.REPO_ID.eq(repoId.getIdAsString()))
				.and(RUN.COMMIT_HASH.eq(commitHash.getHash()))
				.orderBy(RUN.START_TIME.desc())
				.stream()
				.map(RunRecord::getId)
				.map(RunId::fromString)
				.collect(toList());
		}
	}

	/**
	 * Get the ids of the most recent runs ordered by their commit's committer time. Runs with no
	 * associated commit are ignored. If a commit has multiple runs, they are ordered from new to
	 * old.
	 *
	 * @param skip how many recent runs to skip
	 * @param amount how many recent runs to collect
	 * @return a sorted list containing the recent runs from new to old
	 * @throws IllegalArgumentException if skip/amount is negative
	 */
	public List<RunId> getRecentRunIds(int skip, int amount) throws IllegalArgumentException {
		if (skip < 0) {
			throw new IllegalArgumentException("skip must be positive");
		}
		if (amount < 0) {
			throw new IllegalArgumentException("amount must be positive");
		}
		if (amount == 0) {
			return Collections.emptyList();
		}

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.select(RUN.ID)
				.from(RUN)
				.join(KNOWN_COMMIT)
				.on(KNOWN_COMMIT.REPO_ID.eq(RUN.REPO_ID)
					.and(KNOWN_COMMIT.HASH.eq(RUN.COMMIT_HASH)))
				.where(KNOWN_COMMIT.TRACKED)
				.orderBy(KNOWN_COMMIT.COMMITTER_DATE.desc(), RUN.START_TIME.desc())
				.limit(skip, amount)
				.stream()
				.map(Record1::value1)
				.map(RunId::fromString)
				.collect(toList());
		}
	}

	/**
	 * Find the id of a commit's latest run.
	 *
	 * @param repoId the commit's repo
	 * @param commitHash the commit's hash
	 * @return the id of the commit's latest run, or empty if the commit has no associated run
	 */
	public Optional<RunId> getLatestRunId(RepoId repoId, CommitHash commitHash) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.selectFrom(LATEST_RUN)
				.where(LATEST_RUN.REPO_ID.eq(repoId.getIdAsString()))
				.and(LATEST_RUN.COMMIT_HASH.eq(commitHash.getHash()))
				.fetchOptional()
				.map(record -> RunId.fromString(record.getId()));
		}
	}

	/**
	 * Find the id of the latest run for each commit.
	 *
	 * @param repoId the repo the commits are in
	 * @param commitHashes the commits whose latest runs to find
	 * @return a map containing each commit's latest run's id. If a commit has not associated run, it
	 * 	is omitted from this map.
	 */
	public Map<CommitHash, RunId> getLatestRunIds(RepoId repoId,
		Collection<CommitHash> commitHashes) {

		Set<String> hashStrings = commitHashes.stream()
			.map(CommitHash::getHash)
			.collect(toSet());

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.selectFrom(LATEST_RUN)
				.where(LATEST_RUN.REPO_ID.eq(repoId.getIdAsString()))
				.and(LATEST_RUN.COMMIT_HASH.in(hashStrings))
				.stream()
				.collect(toMap(
					record -> new CommitHash(record.getCommitHash()),
					record -> RunId.fromString(record.getId())
				));
		}
	}

	public List<SearchRunDescription> searchRuns(Integer limit,
		@Nullable RepoId repoId, String queryStr) {

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			var query = db.dsl()
				.select(
					RUN.ID,
					RUN.REPO_ID,
					KNOWN_COMMIT.MESSAGE,
					RUN.TAR_DESC,
					RUN.START_TIME,
					RUN.STOP_TIME
				)
				.from(RUN)
				.leftOuterJoin(KNOWN_COMMIT)
				.on(KNOWN_COMMIT.REPO_ID.eq(RUN.REPO_ID))
				.and(KNOWN_COMMIT.HASH.eq(RUN.COMMIT_HASH))
				.where(RUN.ID.contains(queryStr)
					.or(RUN.AUTHOR.contains(queryStr))
					.or(RUN.RUNNER_NAME.contains(queryStr))
					.or(RUN.RUNNER_INFO.contains(queryStr))
					.or(RUN.TAR_DESC.contains(queryStr)));

			if (repoId != null) {
				query = query.and(RUN.REPO_ID.eq(repoId.getIdAsString()));
			}

			return query.orderBy(RUN.START_TIME.desc())
				.limit(limit)
				.stream()
				.map(record -> new SearchRunDescription(
					RunId.fromString(record.value1()),
					Optional.ofNullable(record.value2()).map(RepoId::fromString).orElse(null),
					Optional.ofNullable(record.value3())
						.map(Commit::splitMessageIntoSections)
						.map(Pair::getFirst)
						.orElse(null),
					record.value4(),
					record.value5(),
					record.value6()
				))
				.collect(toList());
		}
	}

	public ShortRunDescription getShortRunDescription(RunId runId) throws NoSuchRunException {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.select(RUN.COMMIT_HASH, KNOWN_COMMIT.MESSAGE, RUN.TAR_DESC)
				.from(RUN)
				.leftJoin(KNOWN_COMMIT)
				.on(KNOWN_COMMIT.REPO_ID.eq(RUN.REPO_ID))
				.and(KNOWN_COMMIT.HASH.eq(RUN.COMMIT_HASH))
				.where(RUN.ID.eq(runId.getIdAsString()))
				.fetchOptional()
				.map(record -> new ShortRunDescription(
					runId,
					record.value1(),
					record.value2(),
					record.value3()
				))
				.orElseThrow(() -> new NoSuchRunException(runId));
		}
	}

	public long getAmountOfOutdatedRuns() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			Field<Integer> namedField = field("n", Integer.class);
			return db.dsl()
				.select(sum(namedField))
				.from(select(count().minus(1).as(namedField))
					.from(RUN)
					.where(RUN.COMMIT_HASH.isNotNull())
					.groupBy(RUN.COMMIT_HASH))
				.fetchSingle()
				.value1()
				.longValue();
		}
	}

	public long getAmountOfTarRunsNotAttachedToRepo() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.select(count())
				.from(RUN)
				.where(RUN.COMMIT_HASH.isNull())
				.and(RUN.REPO_ID.isNull())
				.fetchSingle()
				.value1();
		}
	}

	public long getAmountOfTarRunsAttachedToRepo() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.select(count())
				.from(RUN)
				.where(RUN.COMMIT_HASH.isNull())
				.and(RUN.REPO_ID.isNotNull())
				.fetchSingle()
				.value1();
		}
	}

	public long getAmountOfFailedRuns() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.select(count())
				.from(RUN)
				.where(RUN.ERROR.isNotNull())
				.fetchSingle()
				.value1();
		}
	}

	public long getAmountOfFailedRunErrorMsgChars() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.select(sum(length(RUN.ERROR)))
				.from(RUN)
				.where(RUN.ERROR.isNotNull())
				.fetchSingle()
				.value1()
				.longValue();
		}
	}

	public long getAmountOfRunsForUntrackedCommits() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.select(count())
				.from(RUN)
				.join(KNOWN_COMMIT)
				.on(KNOWN_COMMIT.REPO_ID.eq(RUN.REPO_ID)
					.and(KNOWN_COMMIT.HASH.eq(RUN.COMMIT_HASH)))
				.where(not(KNOWN_COMMIT.TRACKED))
				.fetchSingle()
				.value1();
		}
	}

	public long getAmountOfRunsForUnreachableCommits() {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.dsl()
				.select(count())
				.from(RUN)
				.join(KNOWN_COMMIT)
				.on(KNOWN_COMMIT.REPO_ID.eq(RUN.REPO_ID)
					.and(KNOWN_COMMIT.HASH.eq(RUN.COMMIT_HASH)))
				.where(not(KNOWN_COMMIT.REACHABLE))
				.fetchSingle()
				.value1();
		}
	}
}
