package de.aaaaaaah.velcom.runner.shared.protocol.serialization.serverbound;

import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Converter;

/**
 * Reply indicating the current result was cleared.
 */
public class ClearResultReply implements ServerBound {

	@Override
	public ServerBoundPacket asPacket(Converter serializer) {
		return new ServerBoundPacket(
			ServerBoundPacketType.CLEAR_RESULT_REPLY,
			serializer.serializeTree(this)
		);
	}
}
