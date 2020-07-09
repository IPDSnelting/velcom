package de.aaaaaaah.velcom.backend.runner_new.single.state;

import de.aaaaaaah.velcom.backend.runner_new.single.RunnerConnection;
import de.aaaaaaah.velcom.backend.runner_new.single.TeleRunner;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.AbortRunReply;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.ServerBoundPacket;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.ServerBoundPacketType;
import java.util.Optional;

/**
 * A state waiting for a reply to the GetResult message.
 */
public class AwaitAbortRunReply extends TimeoutState {

	public AwaitAbortRunReply(TeleRunner runner, RunnerConnection connection) {
		super(runner, connection);
	}

	@Override
	public void onExit() {
		super.onExit();
	}

	@Override
	protected Optional<TeleRunnerState> onPacket(ServerBoundPacket packet) {
		return super.onPacket(packet).or(() -> Optional.of(packet)
			.filter(it -> it.getType() == ServerBoundPacketType.ABORT_RUN_REPLY)
			.flatMap(it -> connection.getSerializer().deserialize(it.getData(), AbortRunReply.class))
			.map(reply -> new IdleState(runner, connection))
		);
	}
}
