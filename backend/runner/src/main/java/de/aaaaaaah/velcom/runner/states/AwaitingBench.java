package de.aaaaaaah.velcom.runner.states;

import de.aaaaaaah.velcom.runner.Connection;
import de.aaaaaaah.velcom.runner.TeleBackend;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.RequestRunReply;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO implement
public class AwaitingBench extends RunnerState {

	private static final Logger LOGGER = LoggerFactory.getLogger(AwaitingBench.class);

	private final CompletableFuture<Boolean> receivedData;
	private boolean receivedDataPassedOn;
	private final RequestRunReply reply;

	public AwaitingBench(TeleBackend teleBackend, Connection connection,
		CompletableFuture<Boolean> receivedData, RequestRunReply reply) {

		super(teleBackend, connection);

		this.receivedData = receivedData;
		receivedDataPassedOn = false;

		this.reply = reply;
	}

	@Override
	public void onEnter() {
		LOGGER.info("Receiving bench repo from " + teleBackend);
	}

	@Override
	public RunnerState onBinary(ByteBuffer data, boolean last) {
		// TODO actually receive data

		if (last) {
			if (reply.hasRun()) {
				receivedDataPassedOn = true;
				return new AwaitingRun(teleBackend, connection, receivedData, reply);
			} else {
				receivedData.complete(false);
				return new Idle(teleBackend, connection);
			}
		} else {
			return this;
		}
	}

	@Override
	public void onExit() {
		if (!receivedDataPassedOn) {
			receivedData.complete(false);
		}
	}
}
