package de.aaaaaaah.velcom.runner.revision.states;

import de.aaaaaaah.velcom.runner.revision.Connection;
import de.aaaaaaah.velcom.runner.revision.Delays;
import de.aaaaaaah.velcom.runner.revision.TeleBackend;
import de.aaaaaaah.velcom.shared.Timeout;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.Converter;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClientBoundPacket;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClientBoundPacketType;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.RequestRunReply;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Waiting for a {@link RequestRunReply} from the server.
 */
public class AwaitingRequestRunReply extends RunnerState {

	private final CompletableFuture<Boolean> receivedData;
	private boolean receivedDataPassedOn;
	private final Timeout timeout;

	public AwaitingRequestRunReply(TeleBackend teleBackend, Connection connection,
		CompletableFuture<Boolean> receivedData) {

		super(teleBackend, connection);

		this.receivedData = receivedData;
		receivedDataPassedOn = false;

		timeout = Timeout.after(Delays.AWAIT_COMMAND_REPLY);
		timeout.getCompletionStage()
			.thenAccept(aVoid -> connection.close(StatusCode.COMMAND_TIMEOUT));
	}

	@Override
	public void onEnter() {
		timeout.start();
	}

	@Override
	protected Optional<RunnerState> onPacket(ClientBoundPacket packet) {
		Converter serializer = connection.getSerializer();

		return super.onPacket(packet).or(() -> Optional.of(packet)
			.filter(p -> p.getType() == ClientBoundPacketType.REQUEST_RUN_REPLY)
			.flatMap(p -> serializer.deserialize(p.getData(), RequestRunReply.class))
			.map(p -> {
				if (p.hasBench()) {
					receivedDataPassedOn = true;
					return new AwaitingBench(teleBackend, connection, receivedData, p);
				} else if (p.hasRun()) {
					receivedDataPassedOn = true;
					return new AwaitingRun(teleBackend, connection, receivedData, p);
				} else {
					return new Idle(teleBackend, connection);
				}
			})
		);
	}

	@Override
	public void onExit() {
		timeout.cancel();

		if (!receivedDataPassedOn) {
			receivedData.complete(false);
		}
	}
}
