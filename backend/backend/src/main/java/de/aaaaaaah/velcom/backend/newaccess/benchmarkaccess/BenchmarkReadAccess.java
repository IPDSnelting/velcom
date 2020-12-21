package de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.jooq.codegen.db.Tables.MEASUREMENT;
import static org.jooq.codegen.db.Tables.MEASUREMENT_VALUE;
import static org.jooq.codegen.db.Tables.RUN;
import static org.jooq.impl.DSL.max;

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
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DBReadAccess;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.shared.util.Either;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jooq.codegen.db.tables.records.RunRecord;

/**
 * Access for runs and their measurements.
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

	public Run getRun(RunId runId) {
		return null; // TODO: 20.12.20 Implement
	}

	public List<Run> getRuns(Collection<RunId> runIds) {
		return null; // TODO: 21.12.20 Implement
	}

	public List<Run> getAllRuns(RepoId repoId, CommitHash hash) {
		return null; // TODO: 20.12.20 Implement
	}

	public List<Run> getRecentRuns(int skip, int batchSize) {
		return null; // TODO: 20.12.20 Implement
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
			return db.selectFrom(RUN)
				.where(RUN.REPO_ID.eq(repoId.getIdAsString()))
				.and(RUN.COMMIT_HASH.eq(commitHash.getHash()))
				.orderBy(RUN.START_TIME.desc())
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
			return db.select(
				RUN.COMMIT_HASH,
				RUN.ID,
				max(RUN.START_TIME) // Sqlite-specific, grab latest run
			)
				.from(RUN)
				.where(RUN.REPO_ID.eq(repoId.getIdAsString()))
				.and(RUN.COMMIT_HASH.in(hashStrings))
				.groupBy(RUN.COMMIT_HASH)
				.stream()
				.collect(toMap(
					record -> new CommitHash(record.value1()),
					record -> RunId.fromString(record.value2())
				));
		}
	}

//	public Run getRun(RunId runId) throws NoSuchRunException {
//		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
//			RunRecord runRecord = db.fetchOne(RUN, RUN.ID.eq(runId.getIdAsString()));
//			return runRecordToRun(runRecord);
//		} catch (DataAccessException e) {
//			throw new NoSuchRunException(e, runId);
//		}
//	}
//
//	public List<Run> getRuns(RunId runId) {
//
//	}
//
//	/**
//	 * Works like {@link #getRun(RunId)} in that a {@link NoSuchRunException} is thrown if no run with
//	 * the specified id exists. This method is meant for the cases where a run must exist but more
//	 * detail is not needed.
//	 *
//	 * @param runId the id of the run that must exist
//	 * @throws NoSuchRunException if no run with that id exists
//	 */
//	public void guardRunExists(RunId runId) throws NoSuchRunException {
//		getRun(runId);
//	}
//
//	public List<Run> getRecentRuns(int skip, int amount) throws IllegalArgumentException {
//		if (skip < 0) {
//			throw new IllegalArgumentException("skip must be positive");
//		}
//		if (amount < 0) {
//			throw new IllegalArgumentException("amount must be positive");
//		}
//		if (amount == 0) {
//			return Collections.emptyList();
//		}
//
//		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
//			return db.selectFrom(RUN)
//				.orderBy(RUN.START_TIME.desc())
//				.limit(skip, amount)
//				.stream()
//				.map(BenchmarkReadAccess::runRecordToRun)
//				.collect(Collectors.toList());
//		}
//	}
//
//	/**
//	 * Get all runs of a specific commit, ordered from most to least recent.
//	 *
//	 * @param repoId the id of the repository the commit is in
//	 * @param commitHash the hash of the commit
//	 * @return all runs for the commit, ordered from most to least recent
//	 */
//	public List<Run> getAllRuns(RepoId repoId, CommitHash commitHash) {
//		try (DBReadAccess db = databaseStorage.acquireReadAccess()) {
//			return db.selectFrom(RUN)
//				.where(RUN.REPO_ID.eq(repoId.getIdAsString()))
//				.and(RUN.COMMIT_HASH.eq(commitHash.getHash()))
//				.orderBy(RUN.START_TIME.desc())
//				.stream()
//				.map(BenchmarkReadAccess::runRecordToRun)
//				.collect(Collectors.toList());
//		}
//	}
}
