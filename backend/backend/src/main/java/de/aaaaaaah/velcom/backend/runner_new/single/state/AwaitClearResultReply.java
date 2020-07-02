package de.aaaaaaah.velcom.backend.runner_new.single.state;

import de.aaaaaaah.velcom.backend.runner_new.single.RunnerConnection;
import de.aaaaaaah.velcom.backend.runner_new.single.TeleRunner;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound.ClientBoundPacket;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound.ClientBoundPacketType;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.serverbound.ClearResultReply;
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
	protected Optional<TeleRunnerState> onPacket(ClientBoundPacket packet) {
		return super.onPacket(packet).or(() -> Optional.of(packet)
			.filter(it -> it.getType() == ClientBoundPacketType.CLEAR_RESULT)
			.flatMap(it -> connection.getSerializer().deserialize(it.getData(), ClearResultReply.class))
			.map(reply -> {
				replyFuture.complete(null);

				return new IdleState(runner, connection);
			})
		);
	}
}
