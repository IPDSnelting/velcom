package de.aaaaaaah.velcom.runner.states;

import de.aaaaaaah.velcom.runner.Connection;
import de.aaaaaaah.velcom.runner.Delays;
import de.aaaaaaah.velcom.runner.TeleBackend;
import de.aaaaaaah.velcom.shared.util.Timeout;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClientBoundPacket;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.ClientBoundPacketType;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.RequestRunReply;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Waiting for a {@link RequestRunReply} from the server.
 */
public class AwaitingRequestRunReply extends RunnerState {

	private static final Logger LOGGER = LoggerFactory.getLogger(AwaitingRequestRunReply.class);

	private final CompletableFuture<RequestRunReply> replyFuture;
	private boolean owningReplyFuture;
	private final Timeout timeout;

	public AwaitingRequestRunReply(TeleBackend teleBackend, Connection connection,
		CompletableFuture<RequestRunReply> replyFuture) {

		super(teleBackend, connection);

		this.replyFuture = replyFuture;
		owningReplyFuture = true;

		timeout = Timeout.after(Delays.AWAIT_COMMAND_REPLY);
		timeout.getCompletionStage()
			.thenAccept(aVoid -> connection.close(StatusCode.COMMAND_TIMEOUT));
	}

	@Override
	public void onEnter() {
		LOGGER.debug("Waiting for request_run_reply from {}", teleBackend);
		timeout.start();
	}

	@Override
	protected Optional<RunnerState> onPacket(ClientBoundPacket packet) {
		Serializer serializer = connection.getSerializer();

		return super.onPacket(packet).or(() -> Optional.of(packet)
			.filter(p -> p.getType() == ClientBoundPacketType.REQUEST_RUN_REPLY)
			.flatMap(p -> serializer.deserialize(p.getData(), RequestRunReply.class))
			.map(p -> {
				LOGGER.debug("{}: hasBench {}, hasRun {}", teleBackend, p.hasBench(), p.hasRun());
				if (p.hasBench()) {
					owningReplyFuture = false;
					return new AwaitingBench(teleBackend, connection, p, replyFuture);
				} else if (p.hasRun()) {
					owningReplyFuture = false;
					return new AwaitingRun(teleBackend, connection, p, replyFuture);
				} else {
					return new Idle(teleBackend, connection);
				}
			})
		);
	}

	@Override
	public void onExit() {
		timeout.cancel();

		if (owningReplyFuture) {
			replyFuture.cancel(true);
		}
	}
}
