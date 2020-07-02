package de.aaaaaaah.velcom.backend.runner_new.single.state;

import de.aaaaaaah.velcom.backend.runner_new.single.RunnerConnection;
import de.aaaaaaah.velcom.backend.runner_new.single.TeleRunner;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound.ClientBoundPacket;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound.ClientBoundPacketType;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.serverbound.GetStatusReply;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A state waiting for a reply to the GetStatus message.
 */
public class AwaitGetStatusReply extends TimeoutState {

	private final CompletableFuture<GetStatusReply> replyFuture;

	public AwaitGetStatusReply(TeleRunner runner, RunnerConnection connection) {
		super(runner, connection);

		this.replyFuture = new CompletableFuture<>();
	}

	/**
	 * @return the reply future.
	 */
	public CompletableFuture<GetStatusReply> getReplyFuture() {
		return replyFuture;
	}

	@Override
	public void onExit() {
		super.onExit();

		if (!replyFuture.isDone()) {
			replyFuture.cancel(true);
		}
	}

	@Override
	protected Optional<TeleRunnerState> onPacket(ClientBoundPacket packet) {
		return super.onPacket(packet).or(() -> Optional.of(packet)
			.filter(it -> it.getType() == ClientBoundPacketType.GET_STATUS)
			.flatMap(it -> connection.getSerializer().deserialize(it.getData(), GetStatusReply.class))
			.map(reply -> {
				replyFuture.complete(reply);

				return new IdleState(runner, connection);
			})
		);
	}
}
