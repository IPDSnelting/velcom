package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;

public class AbortRun implements ClientBound {

	@Override
	public ClientBoundPacket asPacket(Serializer serializer) {
		return new ClientBoundPacket(
			ClientBoundPacketType.ABORT_RUN,
			serializer.serializeTree(this)
		);
	}
}
