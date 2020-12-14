package de.aaaaaaah.velcom.shared.protocol.serialization.clientbound;

import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;

/**
 * The client should abort the run it is currently doing for the backend. The client doesn't give
 * any guarantees to actually abort the run. If the client isn't performing a run for the backend,
 * this has no effect.
 */
public class AbortRun implements ClientBound {

	@Override
	public ClientBoundPacket asPacket(Serializer serializer) {
		return new ClientBoundPacket(
			ClientBoundPacketType.ABORT_RUN,
			serializer.serializeTree(this)
		);
	}
}
