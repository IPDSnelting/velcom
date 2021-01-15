package de.aaaaaaah.velcom.runner.benchmarking;

import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.util.Either;
import java.time.Instant;
import java.util.UUID;

/**
 * The result of trying to do a run on the runner.
 */
public class BenchResult {

	private final UUID runId;
	private final Either<String, Result> result;
	private final Instant startTime;
	private final Instant stopTime;

	private BenchResult(UUID runId, Either<String, Result> result, Instant startTime,
		Instant stopTime) {

		this.runId = runId;
		this.result = result;
		this.startTime = startTime;
		this.stopTime = stopTime;
	}

	/**
	 * Create a new successful benchmark result. A bench result is successful if the runner executed
	 * the bench script correctly and the bench script printed valid output (even a global error).
	 *
	 * @param runId the run's id
	 * @param result the parsed bench script output
	 * @param startTime when the run started
	 * @param stopTime when the run finished
	 * @return the newly created bench result
	 */
	public static BenchResult successful(UUID runId, Result result, Instant startTime,
		Instant stopTime) {

		return new BenchResult(runId, Either.ofRight(result), startTime, stopTime);
	}

	/**
	 * Create a new failed benchmark result. A bench result is failed if the runner could not execute
	 * the bench script correctly or the bench script printed invalid output.
	 *
	 * @param runId the run's id
	 * @param error a string describing the failure
	 * @param startTime when the run was started
	 * @param stopTime when the run was finished
	 * @return the newly created bench result
	 */
	public static BenchResult failed(UUID runId, String error, Instant startTime, Instant stopTime) {
		return new BenchResult(runId, Either.ofLeft(error), startTime, stopTime);
	}

	public UUID getRunId() {
		return runId;
	}

	public boolean isSuccess() {
		return result.isRight();
	}

	public Either<String, Result> getResult() {
		return result;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getStopTime() {
		return stopTime;
	}
}
