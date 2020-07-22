package de.aaaaaaah.velcom.runner.revision.states;

import com.fasterxml.jackson.databind.JsonNode;
import de.aaaaaaah.velcom.runner.revision.Connection;
import de.aaaaaaah.velcom.runner.revision.TeleBackend;
import de.aaaaaaah.velcom.runner.revision.benchmarking.BenchResult;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.Converter;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.AbortRun;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClearResult;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClientBoundPacket;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.GetResult;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.GetStatus;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.AbortRunReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.ClearResultReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetResultReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.GetStatusReply;
import de.aaaaaaah.velcom.shared.protocol.statemachine.State;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * A {@link RunnerState} can receive different kinds of websocket packets and connection events.
 * Because all such states should respond to all server commands immediately, this is an abstract
 * class implementing that behaviour and not just an interface.
 */
public abstract class RunnerState implements State {

	protected final TeleBackend teleBackend;
	protected final Connection connection;

	public RunnerState(TeleBackend teleBackend, Connection connection) {
		this.teleBackend = teleBackend;
		this.connection = connection;
	}

	/**
	 * Called when a complete text packet has been received via the websocket connection.
	 *
	 * @param text The contents of the received packet
	 * @return the state to switch to next
	 */
	public RunnerState onText(String text) {
		Optional<RunnerState> newState = connection.getSerializer()
			.deserialize(text, ClientBoundPacket.class)
			.flatMap(this::onPacket);

		// If a packet has been received that could not be deserialized or handled, that is invalid
		// behaviour.
		if (newState.isEmpty()) {
			connection.close(StatusCode.ILLEGAL_PACKET);
		}

		return newState.orElse(this);
	}

	/**
	 * Called by the default {@link #onText(String)} implementation if the text could be decoded to
	 * a {@link ClientBoundPacket}.
	 *
	 * @param packet the packet the text was deserialized into
	 * @return whether this function call handled the packet
	 */
	protected Optional<RunnerState> onPacket(ClientBoundPacket packet) {
		Converter serializer = connection.getSerializer();
		JsonNode data = packet.getData();

		switch (packet.getType()) {
			case GET_STATUS:
				return serializer.deserialize(data, GetStatus.class).flatMap(this::onGetStatus);

			case GET_RESULT:
				return serializer.deserialize(data, GetResult.class).flatMap(this::onGetResult);

			case CLEAR_RESULT:
				return serializer.deserialize(data, ClearResult.class).flatMap(this::onClearResult);

			case ABORT_RUN:
				return serializer.deserialize(data, AbortRun.class).flatMap(this::onAbortRun);

			default:
				return Optional.empty();
		}
	}

	protected Optional<RunnerState> onGetStatus(GetStatus getStatus) {
		GetStatusReply getStatusReply = new GetStatusReply(
			teleBackend.getInfo().format(),
			teleBackend.getBenchHash().orElse(null),
			teleBackend.getBenchResult().isPresent(),
			teleBackend.getStatus(),
			teleBackend.getCurrentRunId().orElse(null)
		);
		connection.sendPacket(getStatusReply.asPacket(connection.getSerializer()));

		return Optional.of(this);
	}

	protected Optional<RunnerState> onGetResult(GetResult getResult) {
		Optional<BenchResult> resultOptional = teleBackend.getBenchResult();
		if (resultOptional.isEmpty()) {
			connection.close(StatusCode.NO_RESULT);
			return Optional.empty();
		}
		BenchResult result = resultOptional.get();

		GetResultReply getResultReply = new GetResultReply(result.getRunId(), result.isSuccess(),
			result.getResult(), result.getError(), result.getStartTime(), result.getStopTime());
		connection.sendPacket(getResultReply.asPacket(connection.getSerializer()));

		return Optional.of(this);
	}

	protected Optional<RunnerState> onClearResult(ClearResult clearResult) {
		if (!teleBackend.clearBenchResult()) {
			connection.close(StatusCode.NO_RESULT);
			return Optional.empty();
		}
		connection.sendPacket(new ClearResultReply().asPacket(connection.getSerializer()));

		return Optional.of(this);
	}

	protected Optional<RunnerState> onAbortRun(AbortRun abortRun) {
		teleBackend.abortCurrentRun();
		connection.sendPacket(new AbortRunReply().asPacket(connection.getSerializer()));

		return Optional.of(this);
	}

	/**
	 * Called when (part of) a binary packet has been received via the websocket connection. The
	 * {@link ByteBuffer} must not be used any further after this function returns!
	 *
	 * @param data the data that was received
	 * @param last whether this invocation completes the message
	 * @return the state to switch to next
	 */
	public RunnerState onBinary(ByteBuffer data, boolean last) {
		// Binary packets are only expected in certain circumstances, but usually they are invalid
		// behaviour.
		connection.close(StatusCode.ILLEGAL_BINARY_PACKET);
		return this;
	}
}
