package de.aaaaaaah.velcom.backend.access.benchmark;

import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A basic builder that allows the creation of new runs.
 */
public class RunBuilder {

	/**
	 * Creates a new run builder for successful runs.
	 *
	 * @param repoId the id of the repository
	 * @param commitHash the hash of the commit
	 * @param startTime the start time
	 * @param stopTime the stop time
	 * @return a new builder instance
	 */
	public static RunBuilder successful(RepoId repoId, CommitHash commitHash, Instant startTime,
		Instant stopTime) {
		return new RunBuilder(repoId, commitHash, startTime, stopTime, null);
	}

	/**
	 * Creates a new run builder for failed runs
	 *
	 * @param repoId the id of the repository
	 * @param commitHash the hash of the commit
	 * @param startTime the start time
	 * @param stopTime the stop time
	 * @param errorMessage the error message
	 * @return a new builder instance
	 */
	public static RunBuilder failed(RepoId repoId, CommitHash commitHash, Instant startTime,
		Instant stopTime, String errorMessage) {
		return new RunBuilder(repoId, commitHash, startTime, stopTime, errorMessage);
	}

	private final RunId id = new RunId();
	private final RepoId repoId;
	private final CommitHash commitHash;
	private final Instant startTime;
	private final Instant stopTime;
	private final @Nullable
	String errorMessage;

	private final List<TmpMeasurement> measurements;

	RunBuilder(RepoId repoId, CommitHash commitHash, Instant startTime,
		Instant stopTime, @Nullable String errorMessage) {
		this.repoId = repoId;
		this.commitHash = commitHash;
		this.startTime = startTime;
		this.stopTime = stopTime;
		this.errorMessage = errorMessage;
		this.measurements = new ArrayList<>();
	}

	/**
	 * Adds a new successful measurement to the run.
	 *
	 * @param name the name of the measurement
	 * @param interpretation the interpretation
	 * @param unit the unit
	 * @param values the values
	 */
	public void addSuccessfulMeasurement(MeasurementName name, Interpretation interpretation,
		Unit unit, List<Double> values) {

		measurements.add(new TmpMeasurement(
			id,
			name,
			unit,
			interpretation,
			values
		));
	}

	/**
	 * Adds a new failed measurement to the run.
	 *
	 * @param name the name of the measurement
	 * @param errorMessage the error message
	 */
	public void addFailedMeasurement(MeasurementName name, String errorMessage) {
		measurements.add(new TmpMeasurement(
			id,
			name,
			null,
			null,
			errorMessage
		));
	}

	RunId getId() {
		return id;
	}

	RepoId getRepoId() {
		return repoId;
	}

	CommitHash getCommitHash() {
		return commitHash;
	}

	Instant getStartTime() {
		return startTime;
	}

	Instant getStopTime() {
		return stopTime;
	}

	List<TmpMeasurement> getMeasurements() {
		return measurements;
	}

	@Nullable
	String getErrorMessage() {
		return errorMessage;
	}

}
