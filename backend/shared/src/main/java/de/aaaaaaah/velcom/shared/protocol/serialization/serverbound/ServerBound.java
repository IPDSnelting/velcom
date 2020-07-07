package de.aaaaaaah.velcom.shared.protocol.serialization.serverbound;

import de.aaaaaaah.velcom.shared.protocol.serialization.Converter;

/**
 * Packets that can be converted into a {@link ServerBoundPacket}.
 */
public interface ServerBound {

	/**
	 * Convert this packet to a {@link ServerBoundPacket}.
	 *
	 * @param serializer the JSON serializer instance to use
	 * @return the {@link ServerBoundPacket}
	 */
	ServerBoundPacket asPacket(Converter serializer);

}
