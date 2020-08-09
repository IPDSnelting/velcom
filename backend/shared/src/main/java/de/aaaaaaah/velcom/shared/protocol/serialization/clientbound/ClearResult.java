package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;

/**
 * A command to clear the current result. Must only be sent if the runner currently holds a result.
 */
public class ClearResult implements ClientBound {

	@Override
	public ClientBoundPacket asPacket(Serializer serializer) {
		return new ClientBoundPacket(
			ClientBoundPacketType.CLEAR_RESULT,
			serializer.serializeTree(this)
		);
	}
}
