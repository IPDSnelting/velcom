package de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound;

import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Converter;

/**
 * A command to clear the current result. Must only be sent if the runner currently holds a result.
 */
public class ClearResult implements ClientBound {

	@Override
	public ClientBoundPacket asPacket(Converter serializer) {
		return new ClientBoundPacket(
			ClientBoundPacketType.CLEAR_RESULT,
			serializer.serializeTree(this)
		);
	}
}
