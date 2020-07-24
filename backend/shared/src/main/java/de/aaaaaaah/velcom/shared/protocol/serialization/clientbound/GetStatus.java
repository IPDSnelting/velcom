package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;

/**
 * A command requesting the runner's current status.
 */
public class GetStatus implements ClientBound {

	@Override
	public ClientBoundPacket asPacket(Serializer serializer) {
		return new ClientBoundPacket(
			ClientBoundPacketType.GET_STATUS,
			serializer.serializeTree(this)
		);
	}
}
