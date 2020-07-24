package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClearResult;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Reply containing the runner's current result. To clear the result, use a {@link ClearResult}
 * command.
 */
public class GetResultReply implements ServerBound {

	private final UUID runId;
	private final boolean success;
	@Nullable
	private final Result result;
	@Nullable
	private final String error;
	private final Instant startTime;
	private final Instant stopTime;

	@JsonCreator
	public GetResultReply(
		@JsonProperty(required = true) UUID runId,
		@JsonProperty(required = true) boolean success,
		@Nullable Result result,
		@Nullable String error,
		@JsonProperty(required = true) Instant startTime,
		@JsonProperty(required = true) Instant stopTime) {
		if (success && (result == null || error != null)) {
			throw new IllegalArgumentException(
				"if successful, there must be a result and no error");
		} else if (!success && (result != null || error == null)) {
			throw new IllegalArgumentException(
				"if not successful, there must be an error and no result");
		}

		this.startTime = startTime;
		this.stopTime = stopTime;
		this.runId = runId;
		this.success = success;
		this.result = result;
		this.error = error;
	}

	public UUID getRunId() {
		return runId;
	}

	public boolean isSuccess() {
		return success;
	}

	public Optional<Result> getResult() {
		return Optional.ofNullable(result);
	}

	public Optional<String> getError() {
		return Optional.ofNullable(error);
	}

	public Instant getStartTime() {
		return startTime;
	}

	public Instant getStopTime() {
		return stopTime;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		GetResultReply that = (GetResultReply) o;
		return success == that.success &&
			runId.equals(that.runId) &&
			Objects.equals(result, that.result) &&
			Objects.equals(error, that.error);
	}

	@Override
	public int hashCode() {
		return Objects.hash(runId, success, result, error);
	}

	@Override
	public ServerBoundPacket asPacket(Serializer serializer) {
		return new ServerBoundPacket(
			ServerBoundPacketType.GET_RESULT_REPLY,
			serializer.serializeTree(this)
		);
	}
}
