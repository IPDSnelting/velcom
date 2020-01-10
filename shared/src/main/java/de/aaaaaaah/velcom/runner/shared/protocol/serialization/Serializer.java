package de.aaaaaaah.velcom.runner.shared.protocol.serialization;

import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.exceptions.SerializationException;

/**
 * A serializer for the runner protocol's text messages.
 */
public interface Serializer {

	/**
	 * Serializes an arbitrary {@link SentEntity}.
	 *
	 * @param o the object
	 * @return the serialized object
	 * @throws SerializationException if serialization was not possible
	 */
	String serialize(SentEntity o) throws SerializationException;

	/**
	 * Returns the type ({@link SentEntity#identifier()}) of a serialized input.
	 *
	 * @param input the serialized input. Must come from {@link #serialize(SentEntity)}
	 * @return the type of the serialized input
	 * @throws SerializationException if an error occurs deserializing the input
	 */
	String peekType(String input) throws SerializationException;

	/**
	 * Deserializes an arbitrary object. The input must come from {@link #serialize(SentEntity)} or
	 * all bets are off
	 *
	 * @param input the serialized input
	 * @param <T> the type of the object
	 * @param type the type of the object tot deserialize
	 * @return the deserialized form
	 * @throws SerializationException if an error occurs deserializing the input
	 */
	<T extends SentEntity> T deserialize(String input, Class<T> type) throws SerializationException;
}
