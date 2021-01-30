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
import static org.jooq.impl.DSL.falseCondition;
import static org.jooq.impl.DSL.trueCondition;

import de.aaaaaaah.velcom.backend.access.AccessUtils;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementError;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.ShortRunDescription;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.access.benchmarkaccess.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.shared.util.Either;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.jooq.Condition;
import org.jooq.Record4;
import org.jooq.ResultQuery;
import org.jooq.codegen.db.tables.records.RunRecord;

/**
 * Access for retrieving runs and their measurements.
 */
public class BenchmarkReadAccess {

	private static final int SEARCH_LIMIT = 500;

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
		Map<String, List<Double>> values = db.select(MEASUREMENT.ID, MEASUREMENT_VALUE.VALUE)
			.from(MEASUREMENT)
			.join(MEASUREMENT_VALUE)
			.on(MEASUREMENT_VALUE.MEASUREMENT_ID.eq(MEASUREMENT.ID))
			.where(MEASUREMENT.RUN_ID.in(runIds))
			// TODO: 20.12.20 Preserve order of values?
			.fetchGroups(MEASUREMENT.ID, MEASUREMENT_VALUE.VALUE);

		// 2. Load measurements
		Map<RunId, List<Measurement>> measurements = db.selectFrom(MEASUREMENT)
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
			List<RunRecord> runRecords = db.selectFrom(RUN)
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
			return db.selectFrom(RUN)
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
	 * Get the ids of the most recent runs ordered by their start time.
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
			return db.selectFrom(RUN)
				.orderBy(RUN.START_TIME.desc())
				.limit(skip, amount)
				.stream()
				.map(RunRecord::getId)
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
			return db.selectFrom(LATEST_RUN)
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
			return db.selectFrom(LATEST_RUN)
				.where(LATEST_RUN.REPO_ID.eq(repoId.getIdAsString()))
				.and(LATEST_RUN.COMMIT_HASH.in(hashStrings))
				.stream()
				.collect(toMap(
					record -> new CommitHash(record.getCommitHash()),
					record -> RunId.fromString(record.getId())
				));
		}
	}

	/**
	 * Search for runs. The logical connection between the various filtering conditions can be thought
	 * of as follows:
	 *
	 * <p> latestRunsOnly && limit && repoId && (commitHash || description) &&
	 * oneOf(orderbyRunStartTimeDesc, orderByCommitterTimeDesc)
	 *
	 * @param latestRunsOnly if set to true, only the latest runs will be returned for commits with
	 * 	multiple runs
	 * @param limit an upper limit on the amount of runs that will be returned. For a maximum amount,
	 * 	see {@link BenchmarkReadAccess#SEARCH_LIMIT}
	 * @param repoId id or part of the id of a repo to which the search should be restricted
	 * @param commitHash hash or part of a hash for which runs should be returned
	 * @param description description (tar description or commit message) for which runs should be
	 * 	returned
	 * @param orderByRunStartTimeDesc order the resulting runs by their start time in descending
	 * 	order
	 * @param orderByCommitterTimeDesc order the resulting runs by their commit's committer time in
	 * 	descending order
	 * @return a list of {@link ShortRunDescription}s for runs matching the specified criteria
	 */
	// TODO: 24.01.21 Add tests for partial commit hash
	// TODO: 24.01.21 Add tests specifying commit hash and description at the same time
	public List<ShortRunDescription> searchRuns(
		boolean latestRunsOnly,
		@Nullable Integer limit,
		@Nullable RepoId repoId,
		@Nullable String commitHash,
		@Nullable String description,
		@Nullable Boolean orderByRunStartTimeDesc,
		@Nullable Boolean orderByCommitterTimeDesc
	) {
		limit = limit == null ? SEARCH_LIMIT : Math.min(limit, SEARCH_LIMIT);

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			if (latestRunsOnly) {

				Condition condition;
				if (commitHash == null && description == null) {
					condition = trueCondition();
				} else {
					condition = falseCondition();
					if (commitHash != null) {
						condition = condition.or(LATEST_RUN.COMMIT_HASH.contains(commitHash));
					}
					if (description != null) {
						condition = condition.or(LATEST_RUN.TAR_DESC.contains(description)
							.or(KNOWN_COMMIT.MESSAGE.contains(description)));
					}
				}

				var query = db
					.select(LATEST_RUN.ID, LATEST_RUN.COMMIT_HASH, KNOWN_COMMIT.MESSAGE, LATEST_RUN.TAR_DESC)
					.from(LATEST_RUN)
					.leftJoin(KNOWN_COMMIT)
					.on(KNOWN_COMMIT.REPO_ID.eq(LATEST_RUN.REPO_ID))
					.and(KNOWN_COMMIT.HASH.eq(LATEST_RUN.COMMIT_HASH))
					.where(condition);

				if (repoId != null) {
					query = query.and(LATEST_RUN.REPO_ID.eq(repoId.getIdAsString()));
				}

				final ResultQuery<Record4<String, String, String, String>> resultQuery;
				if (orderByRunStartTimeDesc != null) {
					resultQuery = query.orderBy(orderByRunStartTimeDesc
						? LATEST_RUN.START_TIME.desc()
						: LATEST_RUN.START_TIME.asc())
						.limit(limit);
				} else if (orderByCommitterTimeDesc != null) {
					resultQuery = query.orderBy(orderByCommitterTimeDesc
						? KNOWN_COMMIT.COMMITTER_DATE.desc()
						: KNOWN_COMMIT.COMMITTER_DATE.asc())
						.limit(limit);
				} else {
					resultQuery = query.limit(limit);
				}

				return resultQuery.stream()
					.map(record -> new ShortRunDescription(
						RunId.fromString(record.value1()),
						record.value2(),
						record.value3(),
						record.value4()
					))
					.collect(toList());
			} else {

				Condition condition;
				if (commitHash == null && description == null) {
					condition = trueCondition();
				} else {
					condition = falseCondition();
					if (commitHash != null) {
						condition = condition.or(RUN.COMMIT_HASH.contains(commitHash));
					}
					if (description != null) {
						condition = condition.or(RUN.TAR_DESC.contains(description)
							.or(KNOWN_COMMIT.MESSAGE.contains(description)));
					}
				}

				var query = db
					.select(RUN.ID, RUN.COMMIT_HASH, KNOWN_COMMIT.MESSAGE, RUN.TAR_DESC)
					.from(RUN)
					.leftJoin(KNOWN_COMMIT)
					.on(KNOWN_COMMIT.REPO_ID.eq(RUN.REPO_ID))
					.and(KNOWN_COMMIT.HASH.eq(RUN.COMMIT_HASH))
					.where(condition);

				if (repoId != null) {
					query = query.and(RUN.REPO_ID.eq(repoId.getIdAsString()));
				}

				final ResultQuery<Record4<String, String, String, String>> resultQuery;
				if (orderByRunStartTimeDesc != null) {
					resultQuery = query.orderBy(orderByRunStartTimeDesc
						? RUN.START_TIME.desc()
						: RUN.START_TIME.asc())
						.limit(limit);
				} else if (orderByCommitterTimeDesc != null) {
					resultQuery = query.orderBy(orderByCommitterTimeDesc
						? KNOWN_COMMIT.COMMITTER_DATE.desc()
						: KNOWN_COMMIT.COMMITTER_DATE.asc())
						.limit(limit);
				} else {
					resultQuery = query.limit(limit);
				}

				return resultQuery.stream()
					.map(record -> new ShortRunDescription(
						RunId.fromString(record.value1()),
						record.value2(),
						record.value3(),
						record.value4()
					))
					.collect(toList());
			}
		}
	}

	public ShortRunDescription getShortRunDescription(RunId runId) throws NoSuchRunException {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.select(RUN.COMMIT_HASH, KNOWN_COMMIT.MESSAGE, RUN.TAR_DESC)
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
}
