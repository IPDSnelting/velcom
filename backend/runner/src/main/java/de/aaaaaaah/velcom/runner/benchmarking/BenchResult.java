package de.aaaaaaah.velcom.runner.benchmarking;

import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.util.Either;
import java.time.Instant;
import java.util.UUID;

public class BenchResult {

	private final UUID runId;
	private Either<String, Result> result;
	private final Instant startTime;
	private final Instant stopTime;

	private BenchResult(UUID runId, Either<String, Result> result, Instant startTime,
		Instant stopTime) {

		this.runId = runId;
		this.result = result;
		this.startTime = startTime;
		this.stopTime = stopTime;
	}

	public static BenchResult successful(UUID runId, Result result, Instant startTime,
		Instant stopTime) {

		return new BenchResult(runId, Either.ofRight(result), startTime, stopTime);
	}

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
