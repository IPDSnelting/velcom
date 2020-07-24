package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;

/**
 * A command requesting the current results from the runner. Must only be sent if the runner
 * currently holds a result.
 */
public class GetResult implements ClientBound {

	@Override
	public ClientBoundPacket asPacket(Serializer serializer) {
		return new ClientBoundPacket(
			ClientBoundPacketType.GET_RESULT,
			serializer.serializeTree(this)
		);
	}
}
