package de.aaaaaaah.velcom.runner.shared.protocol.serialization.serverbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Converter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A reply containing the runner's current status.
 */
public class GetStatusReply implements ServerBound {

	private final String name;
	private final String info;
	@Nullable
	private final String benchHash;
	private final boolean resultAvailable;
	private final String state;
	@Nullable
	private final UUID runId;

	@JsonCreator
	public GetStatusReply(
		@JsonProperty(required = true) String name,
		@JsonProperty(required = true) String info,
		@Nullable String benchHash,
		@JsonProperty(required = true) boolean resultAvailable,
		@JsonProperty(required = true) String state,
		@Nullable UUID runId
	) {
		this.name = name;
		this.info = info;
		this.benchHash = benchHash;
		this.resultAvailable = resultAvailable;
		this.state = state;
		this.runId = runId;
	}

	public String getName() {
		return name;
	}

	public String getInfo() {
		return info;
	}

	public Optional<String> getBenchHash() {
		return Optional.ofNullable(benchHash);
	}

	public boolean isResultAvailable() {
		return resultAvailable;
	}

	public String getState() {
		return state;
	}

	public Optional<UUID> getRunId() {
		return Optional.ofNullable(runId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		GetStatusReply that = (GetStatusReply) o;
		return resultAvailable == that.resultAvailable &&
			name.equals(that.name) &&
			info.equals(that.info) &&
			Objects.equals(benchHash, that.benchHash) &&
			state.equals(that.state) &&
			Objects.equals(runId, that.runId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, info, benchHash, resultAvailable, state, runId);
	}

	@Override
	public ServerBoundPacket asPacket(Converter serializer) {
		return new ServerBoundPacket(
			ServerBoundPacketType.GET_STATUS_REPLY,
			serializer.serializeTree(this)
		);
	}
}