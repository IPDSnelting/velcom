package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;
import de.aaaaaaah.velcom.shared.protocol.serialization.Status;
import de.aaaaaaah.velcom.shared.util.LinesWithOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A reply containing the runner's current status.
 */
public class GetStatusReply implements ServerBound {

	private final String info;
	@Nullable
	private final String versionHash;
	@Nullable
	private final String benchHash;
	private final boolean resultAvailable;
	private final Status status;
	@Nullable
	private final UUID runId;
	@Nullable
	private final LinesWithOffset lastOutputLines;

	@JsonCreator
	public GetStatusReply(
		@JsonProperty(required = true) String info,
		@Nullable String versionHash,
		@Nullable String benchHash,
		@JsonProperty(required = true) boolean resultAvailable,
		@JsonProperty(required = true) Status status,
		@Nullable UUID runId,
		@Nullable LinesWithOffset lastOutputLines
	) {
		this.info = info;
		this.versionHash = versionHash;
		this.benchHash = benchHash;
		this.resultAvailable = resultAvailable;
		this.status = status;
		this.runId = runId;
		this.lastOutputLines = lastOutputLines;
	}

	public String getInfo() {
		return info;
	}

	public Optional<String> getVersionHash() {
		return Optional.ofNullable(versionHash);
	}

	public Optional<String> getBenchHash() {
		return Optional.ofNullable(benchHash);
	}

	public boolean isResultAvailable() {
		return resultAvailable;
	}

	public Status getStatus() {
		return status;
	}

	public Optional<UUID> getRunId() {
		return Optional.ofNullable(runId);
	}

	public Optional<LinesWithOffset> getLastOutputLines() {
		return Optional.ofNullable(lastOutputLines);
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
			info.equals(that.info) &&
			Objects.equals(benchHash, that.benchHash) &&
			status.equals(that.status) &&
			Objects.equals(runId, that.runId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(info, benchHash, resultAvailable, status, runId);
	}

	@Override
	public ServerBoundPacket asPacket(Serializer serializer) {
		return new ServerBoundPacket(
			ServerBoundPacketType.GET_STATUS_REPLY,
			serializer.serializeTree(this)
		);
	}

	@Override
	public String toString() {
		return "GetStatusReply{" +
			"versionHash='" + versionHash + '\'' +
			", benchHash='" + benchHash + '\'' +
			", resultAvailable=" + resultAvailable +
			", status=" + status +
			", runId=" + runId +
			'}';
	}
}
