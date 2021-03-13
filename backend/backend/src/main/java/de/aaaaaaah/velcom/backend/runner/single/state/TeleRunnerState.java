package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.single.RunnerConnection;
import de.aaaaaaah.velcom.backend.runner.single.TeleRunner;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.ServerBoundPacket;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.ServerBoundPacketType;
import de.aaaaaaah.velcom.shared.protocol.statemachine.State;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A state in the tele-runner state machine.
 */
public class TeleRunnerState implements State {

	private static final Logger LOGGER = LoggerFactory.getLogger(TeleRunnerState.class);

	protected final TeleRunner runner;
	protected final RunnerConnection connection;

	public TeleRunnerState(TeleRunner runner, RunnerConnection connection) {
		this.runner = runner;
		this.connection = connection;
	}

	/**
	 * Called when a complete text packet has been received via the websocket connection.
	 *
	 * @param text The contents of the received packet
	 * @return the state to switch to next
	 */
	public TeleRunnerState onText(String text) {
		Optional<TeleRunnerState> newState = connection.getSerializer()
			.deserialize(text, ServerBoundPacket.class)
			.flatMap(this::onPacket);

		// If a packet has been received that could not be deserialized or handled, that is invalid
		// behaviour.
		if (newState.isEmpty()) {
			LOGGER.info(
				"Runner send a package I couldn't handle in {}. Content: '{}'",
				getClass().getSimpleName(), text
			);
			connection.close(StatusCode.ILLEGAL_PACKET);
		}

		return newState.orElse(this);
	}

	/**
	 * Called by the default {@link #onText(String)} implementation if the text could be decoded to a
	 * {@link ServerBoundPacket}.
	 *
	 * @param packet the packet the text was deserialized into
	 * @return whether this function call handled the packet
	 */
	protected Optional<TeleRunnerState> onPacket(ServerBoundPacket packet) {
		if (packet.getType() == ServerBoundPacketType.REQUEST_RUN) {
			runner.prepareAndSendWork();
			return Optional.of(this);
		}

		return Optional.empty();
	}
}
