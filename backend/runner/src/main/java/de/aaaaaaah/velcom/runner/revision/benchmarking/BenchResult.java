package de.aaaaaaah.velcom.runner.revision.benchmarking;

import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.Nullable;

public class BenchResult {

	// TODO consider using Either?

	private final UUID runId;
	private final boolean success;
	@Nullable
	private final Result result;
	@Nullable
	private final String error;
	private final Instant startTime;
	private final Instant stopTime;

	public BenchResult(UUID runId, boolean success, @Nullable Result result,
		@Nullable String error, Instant startTime, Instant stopTime) {

		if (success && (result == null || error != null)) {
			throw new IllegalArgumentException(
				"if successful, there must be a result and no error");
		} else if (!success && (result != null || error == null)) {
			throw new IllegalArgumentException(
				"if unsuccessful, there must be an error and no result");
		}

		this.runId = runId;
		this.success = success;
		this.result = result;
		this.error = error;
		this.startTime = startTime;
		this.stopTime = stopTime;
	}

	public UUID getRunId() {
		return runId;
	}

	public boolean isSuccess() {
		return success;
	}

	@Nullable
	public Result getResult() {
		return result;
	}

	@Nullable
	public String getError() {
		return error;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getStopTime() {
		return stopTime;
	}
}
