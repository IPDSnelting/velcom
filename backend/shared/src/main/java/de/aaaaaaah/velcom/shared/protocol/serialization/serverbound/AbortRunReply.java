package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import de.aaaaaaah.velcom.shared.protocol.serialization.Converter;

public class AbortRunReply implements ServerBound {

	@Override
	public ServerBoundPacket asPacket(Converter serializer) {
		return new ServerBoundPacket(
			ServerBoundPacketType.ABORT_RUN_REPLY,
			serializer.serializeTree(this)
		);
	}
}