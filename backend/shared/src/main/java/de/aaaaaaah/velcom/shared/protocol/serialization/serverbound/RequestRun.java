package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import de.aaaaaaah.velcom.shared.protocol.serialization.Converter;

/**
 * A command that allows the backend to send the runner a new benchmark repo version and a new repo
 * to benchmark.
 */
public class RequestRun implements ServerBound {

	@Override
	public ServerBoundPacket asPacket(Converter serializer) {
		return new ServerBoundPacket(
			ServerBoundPacketType.REQUEST_RUN,
			serializer.serializeTree(this)
		);
	}
}
