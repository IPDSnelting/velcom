package de.aaaaaaah.velcom.backend.access.benchmark;

import de.aaaaaaah.velcom.backend.access.AccessLayer;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class abstracts away access to the benchmark results such as runs and measurements.
 */
public class BenchmarkAccess {

	private final AccessLayer accessLayer;
	private final DatabaseStorage databaseStorage;

	/**
	 * This constructor also registers the {@link BenchmarkAccess} in the accessLayer.
	 *
	 * @param accessLayer the {@link AccessLayer} to register with
	 * @param databaseStorage a database storage
	 */
	public BenchmarkAccess(AccessLayer accessLayer, DatabaseStorage databaseStorage) {
		this.accessLayer = accessLayer;
		this.databaseStorage = databaseStorage;

		accessLayer.registerBenchmarkAccess(this);
	}

	// Run ac

	/*
	Basic querying
	 */

	public Run getRun(RunId runId) {
		// TODO implement
		return null;
	}

	public Optional<Run> getLatestRunOf(Commit commit) {
		return getLatestRunOf(commit.getRepoId(), commit.getHash());
	}

	public Optional<Run> getLatestRunOf(RepoId repoId, CommitHash commitHash) {
		// TODO implement
		return null;
	}

	/*
	Advanced operations
	 */

	/**
	 * Add a new successful run to the db.
	 *
	 * @param repoId the repo the commit is in
	 * @param commitHash the hash of the benchmarked commit
	 * @param startTime the time the benchmark script execution was started
	 * @param stopTime the time the benchmark script finished executing
	 * @return the newly created {@link Run}
	 */
	public Run addRun(RepoId repoId, CommitHash commitHash, Instant startTime, Instant stopTime) {
		// TODO implement
		return null;
	}

	/**
	 * Add a new failed run to the db.
	 *
	 * @param repoId the repo the commit is in
	 * @param commitHash the hash of the benchmarked commit
	 * @param startTime the time the benchmark script execution was started
	 * @param stopTime the time the benchmark script finished executing
	 * @param errorMessage the message with which the run failed
	 * @return the newly created {@link Run}
	 */
	public Run addFailedRun(RepoId repoId, CommitHash commitHash, Instant startTime,
		Instant stopTime, String errorMessage) {

		// TODO implement
		return null;
	}

	/**
	 * Add a new successful measurement to an existing run.
	 *
	 * @param runId the run's id
	 * @param measurementName the measurement identification
	 * @param values the measured values
	 * @param interpretation the values' interpretation
	 * @param unit the values' unit
	 * @return the newly created {@link Measurement}
	 */
	public Measurement addMeasurement(RunId runId, MeasurementName measurementName,
		List<Double> values, Interpretation interpretation, Unit unit) {

		// TODO implement
		return null;
	}


	/**
	 * Add a new failed measurement to an existing run.
	 *
	 * @param runId the run's id
	 * @param measurementName the measurement identification
	 * @param errorMessage the message with which the specific measurement failed
	 * @return the newly created {@link Measurement}
	 */
	public Measurement addFailedMeasurement(RunId runId, MeasurementName measurementName,
		String errorMessage) {

		// TODO implement
		return null;
	}

	// TODO mention that the stream needs to be closed after use
	public Stream<Run> getRecentRuns() {
		// TODO implement
		return null;
	}

	/**
	 * Delete all measurements of the specified name from a repo.
	 *
	 * <p> This is useful when a measurement becomes outdated or is renamed.
	 *
	 * @param repoId the repo to delete the measurements from
	 * @param measurementName the name specifying which measurements to delete.
	 */
	public void deleteAllMeasurementsOfName(RepoId repoId, MeasurementName measurementName) {
		// TODO implement
	}

	/**
	 * Delete all measurements and runs that don't need to be kept any more. These are:
	 * <ol>
	 *     <li>runs whose repo doesn't exist any more</li>
	 *     <li>non-failed runs without measurements</li>
	 *     <li>measurements whose run doesn't exist any more (after 1. was applied)</li>
	 * </ol>
	 */
	public void deleteAllUnused() {
		// TODO implement
	}

	public Collection<Measurement> getMeasurements(RunId id) {
		// TODO implement
		return null;
	}

	/**
	 * @param repoId the repo's id
	 * @return all {@link MeasurementName}s of which at least one measurement exists for this repo
	 */
	public Collection<MeasurementName> getAvailableMeasurements(RepoId repoId) {
		// TODO implement
		return null;
	}

}
