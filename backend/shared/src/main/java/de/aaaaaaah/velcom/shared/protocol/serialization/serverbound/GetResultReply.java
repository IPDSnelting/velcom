package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.shared.protocol.serialization.Converter;
import de.aaaaaaah.velcom.shared.protocol.serialization.Result;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClearResult;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Reply containing the runner's current result. To clear the result, use a {@link
 * ClearResult} command.
 */
public class GetResultReply implements ServerBound {

	private final UUID runId;
	private final boolean success;
	@Nullable
	private final Result result;
	@Nullable
	private final String error;

	@JsonCreator
	public GetResultReply(
		@JsonProperty(required = true) UUID runId,
		@JsonProperty(required = true) boolean success,
		@Nullable Result result,
		@Nullable String error
	) {
		if (success && (result == null || error != null)) {
			throw new IllegalArgumentException(
				"if successful, there must be a result and no error");
		} else if (!success && (result != null || error == null)) {
			throw new IllegalArgumentException(
				"if not successful, there must be an error and no result");
		}

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

	@Nullable
	public Optional<Result> getResult() {
		return Optional.ofNullable(result);
	}

	@Nullable
	public Optional<String> getError() {
		return Optional.ofNullable(error);
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
	public ServerBoundPacket asPacket(Converter serializer) {
		return new ServerBoundPacket(
			ServerBoundPacketType.GET_RESULT_REPLY,
			serializer.serializeTree(this)
		);
	}
}
