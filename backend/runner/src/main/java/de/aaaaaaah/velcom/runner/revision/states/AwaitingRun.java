package de.aaaaaaah.velcom.runner.revision.states;

import de.aaaaaaah.velcom.runner.revision.Connection;
import de.aaaaaaah.velcom.runner.revision.Delays;
import de.aaaaaaah.velcom.runner.revision.TeleBackend;
import de.aaaaaaah.velcom.shared.Timeout;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.RequestRunReply;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO implement
public class AwaitingRun extends RunnerState {

	private static final Logger LOGGER = LoggerFactory.getLogger(AwaitingRun.class);

	private final CompletableFuture<Boolean> receivedData;
	private final RequestRunReply reply;

	private final Timeout initialTimeout;
	@Nullable
	private Timeout sequentialTimeout;

	public AwaitingRun(TeleBackend teleBackend, Connection connection,
		CompletableFuture<Boolean> receivedData, RequestRunReply reply) {

		super(teleBackend, connection);

		this.receivedData = receivedData;
		this.reply = reply;

		initialTimeout = Timeout.after(Delays.FOLLOW_UP_PACKET);
		initialTimeout.getCompletionStage()
			.thenAccept(aVoid -> connection.close(StatusCode.COMMAND_TIMEOUT));
	}

	@Override
	public void onEnter() {
		LOGGER.info("Receiving task repo from " + teleBackend);
		initialTimeout.start();
	}

	@Override
	public RunnerState onBinary(ByteBuffer data, boolean last) {
		initialTimeout.cancel();
		if (sequentialTimeout != null) {
			sequentialTimeout.cancel();
		}
		sequentialTimeout = Timeout.after(Delays.NEXT_PARTIAL_BINARY_PACKET);
		sequentialTimeout.getCompletionStage()
			.thenAccept(aVoid -> connection.close(StatusCode.COMMAND_TIMEOUT));

		// TODO actually receive data

		if (last) {
			receivedData.complete(true);
			return new Idle(teleBackend, connection);
		} else {
			return this;
		}
	}

	@Override
	public void onExit() {
		initialTimeout.cancel();
		if (sequentialTimeout != null) {
			sequentialTimeout.cancel();
		}

		receivedData.complete(false);
	}
}
