package de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound;

import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Converter;

/**
 * A command requesting the current results from the runner. Must only be sent if the runner
 * currently holds a result.
 */
public class GetResult implements ClientBound {

	@Override
	public ClientBoundPacket asPacket(Converter serializer) {
		return new ClientBoundPacket(
			ClientBoundPacketType.GET_RESULT,
			serializer.serializeTree(this)
		);
	}
}
