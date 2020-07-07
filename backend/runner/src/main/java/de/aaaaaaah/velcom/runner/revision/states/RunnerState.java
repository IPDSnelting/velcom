package de.aaaaaaah.velcom.runner.revision.states;

import de.aaaaaaah.velcom.runner.revision.TeleBackend;
import de.aaaaaaah.velcom.runner.revision.Connection;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClientBoundPacket;
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
		// TODO respond to all server commands.
		return Optional.empty();
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
