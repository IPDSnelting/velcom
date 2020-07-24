package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.runner.single.RunnerConnection;
import de.aaaaaaah.velcom.backend.runner.single.TeleRunner;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.ClearResultReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.ServerBoundPacket;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.ServerBoundPacketType;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A state waiting for a reply to the GetResult message.
 */
public class AwaitClearResultReply extends TimeoutState {

	private final CompletableFuture<Void> replyFuture;

	public AwaitClearResultReply(TeleRunner runner, RunnerConnection connection) {
		super(runner, connection);

		this.replyFuture = new CompletableFuture<>();
	}

	public CompletableFuture<Void> getReplyFuture() {
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
	protected Optional<TeleRunnerState> onPacket(ServerBoundPacket packet) {
		return super.onPacket(packet).or(() -> Optional.of(packet)
			.filter(it -> it.getType() == ServerBoundPacketType.CLEAR_RESULT_REPLY)
			.flatMap(it -> connection.getSerializer().deserialize(it.getData(), ClearResultReply.class))
			.map(reply -> {
				replyFuture.complete(null);

				return new IdleState(runner, connection);
			})
		);
	}
}
