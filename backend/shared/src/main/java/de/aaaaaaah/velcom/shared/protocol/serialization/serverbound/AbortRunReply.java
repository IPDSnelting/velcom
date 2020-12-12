package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;

/**
 * The reply to {@link de.aaaaaaah.velcom.shared.protocol.serialization.clientbound.AbortRun}.
 * Contains no additional information.
 */
public class AbortRunReply implements ServerBound {

	@Override
	public ServerBoundPacket asPacket(Serializer serializer) {
		return new ServerBoundPacket(
			ServerBoundPacketType.ABORT_RUN_REPLY,
			serializer.serializeTree(this)
		);
	}
}
