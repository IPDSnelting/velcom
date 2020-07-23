package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import de.aaaaaaah.velcom.shared.protocol.serialization.Converter;

/**
 * A command requesting the runner's current status.
 */
public class GetStatus implements ClientBound {

	@Override
	public ClientBoundPacket asPacket(Converter serializer) {
		return new ClientBoundPacket(
			ClientBoundPacketType.GET_STATUS,
			serializer.serializeTree(this)
		);
	}
}
