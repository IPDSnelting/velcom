package de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Converter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A reply indicating what kinds of .tar-s the backend intends to send the runner. Even if the
 * backend currently has no jobs for the runner, it must still send a reply.
 */
public class RequestRunReply implements ClientBound {

	private final boolean bench;
	@Nullable
	private final String benchHash;
	private final boolean run;
	@Nullable
	private final UUID runId;

	@JsonCreator
	public RequestRunReply(
		@JsonProperty(required = true) boolean bench,
		@Nullable String benchHash,
		@JsonProperty(required = true) boolean run,
		@Nullable UUID runId
	) {
		if (bench && benchHash == null) {
			throw new IllegalArgumentException("if bench is true, bench_hash must not be null");
		} else if (!bench && benchHash != null) {
			throw new IllegalArgumentException("if bench is false, bench_hash must be null");
		} else if (run && runId == null) {
			throw new IllegalArgumentException("if run is true, run_id must not be null");
		} else if (!run && runId != null) {
			throw new IllegalArgumentException("if run is false, run_id must be null");
		}

		this.bench = bench;
		this.benchHash = benchHash;
		this.run = run;
		this.runId = runId;
	}

	/**
	 * @return whether the backend will send a new benchmark repo
	 */
	public boolean hasBench() {
		return bench;
	}

	public Optional<String> getBenchHash() {
		return Optional.ofNullable(benchHash);
	}

	/**
	 * @return whether the backend will send a new repo for the runner to benchmark
	 */
	public boolean hasRun() {
		return run;
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
		RequestRunReply that = (RequestRunReply) o;
		return bench == that.bench &&
			run == that.run &&
			Objects.equals(benchHash, that.benchHash) &&
			Objects.equals(runId, that.runId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bench, benchHash, run, runId);
	}

	@Override
	public ClientBoundPacket asPacket(Converter serializer) {
		return new ClientBoundPacket(
			ClientBoundPacketType.REQUEST_RUN_REPLY,
			serializer.serializeTree(this)
		);
	}
}
