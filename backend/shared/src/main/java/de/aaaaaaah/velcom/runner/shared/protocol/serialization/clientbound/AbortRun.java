package de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound;

import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Converter;

public class AbortRun implements ClientBound {

	@Override
	public ClientBoundPacket asPacket(Converter serializer) {
		return new ClientBoundPacket(
			ClientBoundPacketType.ABORT_RUN,
			serializer.serializeTree(this)
		);
	}
}
