package de.aaaaaaah.velcom.runner.revision.states;

import de.aaaaaaah.velcom.runner.revision.TeleBackend;
import de.aaaaaaah.velcom.runner.revision.Connection;
import de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.RequestRunReply;
import java.util.concurrent.CompletableFuture;

// TODO implement
public class AwaitingBench extends RunnerState {

	private final CompletableFuture<Boolean> receivedData;
	private final RequestRunReply reply;

	public AwaitingBench(TeleBackend teleBackend, Connection connection,
		CompletableFuture<Boolean> receivedData, RequestRunReply reply) {

		super(teleBackend, connection);

		this.receivedData = receivedData;
		this.reply = reply;
	}
}
