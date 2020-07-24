package de.aaaaaaah.velcom.runner.states;

import de.aaaaaaah.velcom.runner.Connection;
import de.aaaaaaah.velcom.runner.TeleBackend;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.RequestRunReply;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO implement
public class AwaitingRun extends RunnerState {

	private static final Logger LOGGER = LoggerFactory.getLogger(AwaitingRun.class);

	private final CompletableFuture<Boolean> receivedData;
	private final RequestRunReply reply;

	public AwaitingRun(TeleBackend teleBackend, Connection connection,
		CompletableFuture<Boolean> receivedData, RequestRunReply reply) {

		super(teleBackend, connection);

		this.receivedData = receivedData;
		this.reply = reply;
	}

	@Override
	public void onEnter() {
		LOGGER.info("Receiving task repo from " + teleBackend);
	}

	@Override
	public RunnerState onBinary(ByteBuffer data, boolean last) {
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
		receivedData.complete(false);
	}
}
