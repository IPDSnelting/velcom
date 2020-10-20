package de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess;

import static org.jooq.codegen.db.tables.Run.RUN;
import static org.jooq.impl.DSL.max;

import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunError;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.RunErrorType;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.exceptions.NoSuchRunException;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.shared.util.Either;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jooq.codegen.db.tables.records.RunRecord;
import org.jooq.exception.DataAccessException;

/**
 * Access for runs and their measurements, as well as dimensions.
 *
 * <p> WARNING: This class is not yet meant to be used! Use
 * {@link de.aaaaaaah.velcom.backend.access.BenchmarkReadAccess} instead for now.
 */
public class BenchmarkReadAccess {

	// getDimensionInfos (temporarily)
	// guardDimensionExists (temporarily)
	//
	// getRun (full run)
	// getRecentRuns (full runs)
	// getAllRunsForCommit (full runs)
	// getLatestRunForCommit (full run)
	//
	// timeslice comparison stuff (getLatestRunsForCommits, getMeasurementsForRuns)
	// detail graph stuff (getLatestRunsForCommits, getMeasurementsForRuns)

	protected final DatabaseStorage databaseStorage;

	// TODO: 04.10.20 Change db schema and get rid of this dimension "cache"
	protected final Map<Dimension, DimensionInfo> dimensions;

	public BenchmarkReadAccess(DatabaseStorage databaseStorage) {
		this.databaseStorage = databaseStorage;

		dimensions = new ConcurrentHashMap<>();
	}

	private static Either<CommitSource, TarSource> readSource(@Nullable String repoId,
		@Nullable String commitHash, @Nullable String tarDesc) {

		if (commitHash != null) { // Must be commit source
			return Either.ofLeft(new CommitSource(
				RepoId.fromString(repoId),
				new CommitHash(commitHash)
			));
		} else if (repoId != null) { // Must be tar source with repo id
			return Either.ofRight(new TarSource(
				tarDesc,
				RepoId.fromString(repoId)
			));
		} else { // Must be tar source without repo id
			return Either.ofRight(new TarSource(tarDesc));
		}
	}

	private static Optional<RunError> readError(@Nullable String errorType, @Nullable String error) {
		if (errorType == null) {
			return Optional.empty();
		}

		return Optional.of(new RunError(
			error,
			RunErrorType.fromTextualRepresentation(errorType)
		));
	}

	private static Run runRecordToRun(RunRecord record) {
		return new Run(
			RunId.fromString(record.getId()),
			record.getAuthor(),
			record.getRunnerName(),
			record.getRunnerInfo(),
			record.getStartTime(),
			record.getStopTime(),
			readSource(record.getRepoId(), record.getCommitHash(), record.getTarDesc()),
			readError(record.getErrorType(), record.getError()).orElse(null)
		);
	}

	public Run getRun(RunId runId) throws NoSuchRunException {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			RunRecord runRecord = db.fetchOne(RUN, RUN.ID.eq(runId.getIdAsString()));
			return runRecordToRun(runRecord);
		} catch (DataAccessException e) {
			throw new NoSuchRunException(e, runId);
		}
	}

	public void guardRunExists(RunId runId) throws NoSuchRunException {
		getRun(runId);
	}

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

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectFrom(RUN)
				.orderBy(RUN.START_TIME.desc())
				.limit(skip, amount)
				.stream()
				.map(BenchmarkReadAccess::runRecordToRun)
				.collect(Collectors.toList());
		}
	}

	/**
	 * Get all runs of a specific commit, ordered from most to least recent.
	 *
	 * @param repoId the id of the repository the commit is in
	 * @param commitHash the hash of the commit
	 * @return all runs for the commit, ordered from most to least recent
	 */
	public List<Run> getAllRuns(RepoId repoId, CommitHash commitHash) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectFrom(RUN)
				.where(RUN.REPO_ID.eq(repoId.getIdAsString()))
				.and(RUN.COMMIT_HASH.eq(commitHash.getHash()))
				.orderBy(RUN.START_TIME.desc())
				.stream()
				.map(BenchmarkReadAccess::runRecordToRun)
				.collect(Collectors.toList());
		}
	}

	public Optional<Run> getLatestRun(RepoId repoId, CommitHash commitHash) {
		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.selectFrom(RUN)
				.where(RUN.REPO_ID.eq(repoId.getIdAsString()))
				.and(RUN.COMMIT_HASH.eq(commitHash.getHash()))
				.orderBy(RUN.START_TIME.desc())
				.fetchOptional()
				.map(BenchmarkReadAccess::runRecordToRun);
		}
	}

	public Map<CommitHash, Run> getLatestRuns(RepoId repoId, Collection<CommitHash> commitHashes) {
		Set<String> commitHashStrings = commitHashes.stream()
			.map(CommitHash::getHash)
			.collect(Collectors.toSet());

		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
			return db.select(
				RUN.ID,
				RUN.AUTHOR,
				RUN.RUNNER_NAME,
				RUN.RUNNER_INFO,
				max(RUN.START_TIME), // Sqlite-specific, grab latest run
				RUN.STOP_TIME,
				RUN.REPO_ID,
				RUN.COMMIT_HASH,
				RUN.TAR_DESC,
				RUN.ERROR_TYPE,
				RUN.ERROR
			)
				.from(RUN)
				.where(RUN.REPO_ID.eq(repoId.getIdAsString()))
				.and(RUN.COMMIT_HASH.in(commitHashStrings))
				.groupBy(RUN.COMMIT_HASH)
				.fetchMap(RUN.COMMIT_HASH)
				.entrySet()
				.stream()
				.collect(Collectors.toMap(
					entry -> new CommitHash(entry.getKey()),
					entry -> {
						var record = entry.getValue();
						return new Run(
							RunId.fromString(record.value1()), // id
							record.value2(), // author
							record.value3(), // runner_name
							record.value4(), // runner_info
							record.value5(), // start_time
							record.value6(), // stop_time
							readSource(record.value7(), record.value8(), record.value9()),
							readError(record.value10(), record.value11()).orElse(null)
						);
					}
				));
		}
	}
}
