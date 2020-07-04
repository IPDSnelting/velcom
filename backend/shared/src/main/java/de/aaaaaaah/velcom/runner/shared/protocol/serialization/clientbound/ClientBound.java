package de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound;

import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Converter;

/**
 * Packets that can be converted into a {@link ClientBoundPacket}.
 */
public interface ClientBound {

	/**
	 * Convert this packet to a {@link ClientBoundPacket}.
	 *
	 * @param serializer the JSON serializer instance to use
	 * @return the {@link ClientBoundPacket}
	 */
	ClientBoundPacket asPacket(Converter serializer);
}