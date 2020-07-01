package de.aaaaaaah.velcom.runner.revision.states;

import de.aaaaaaah.velcom.runner.revision.Backend;
import de.aaaaaaah.velcom.runner.revision.Connection;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound.RequestRunReply;
import java.util.concurrent.CompletableFuture;

// TODO implement
public class AwaitingRun extends RunnerState {

	private final CompletableFuture<Boolean> receivedData;
	private final RequestRunReply reply;

	public AwaitingRun(Backend backend, Connection connection,
		CompletableFuture<Boolean> receivedData, RequestRunReply reply) {

		super(backend, connection);

		this.receivedData = receivedData;
		this.reply = reply;
	}
}
