package de.aaaaaaah.velcom.runner.shared.protocol.serialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.exceptions.SerializationException;

/**
 * A simple Jackson based {@link Serializer}.
 */
public class SimpleJsonSerializer implements Serializer {

	private ObjectMapper objectMapper;

	/**
	 * Creates a new simple json serializer.
	 */
	public SimpleJsonSerializer() {
		this.objectMapper = new ObjectMapper()
			.registerModule(new ParameterNamesModule())
			.registerModule(new Jdk8Module())
			.registerModule(new JavaTimeModule());
	}

	@Override
	public String serialize(SentEntity o) throws SerializationException {
		try {
			String serialized = objectMapper.writeValueAsString(o);
			return objectMapper.writeValueAsString(new SentValue(
				serialized, o.identifier()
			));
		} catch (JsonProcessingException e) {
			throw new SerializationException("Error serializing value", e);
		}
	}

	@Override
	public String peekType(String input) throws SerializationException {
		return deserializeIntermediate(input).identifier;
	}

	@Override
	public <T extends SentEntity> T deserialize(String input, Class<T> type)
		throws SerializationException {
		try {
			return objectMapper.readValue(deserializeIntermediate(input).payload, type);
		} catch (JsonProcessingException e) {
			throw new SerializationException("Error deserializing object", e);
		}
	}

	private SentValue deserializeIntermediate(String input)
		throws SerializationException {
		try {
			return objectMapper.readValue(input, SentValue.class);
		} catch (JsonProcessingException e) {
			throw new SerializationException("Error deserializing object", e);
		}
	}

	private static class SentValue {

		private String payload;
		private String identifier;

		/**
		 * Creates a new sent value.
		 *
		 * @param payload the payload (serialized {@link SentEntity})
		 * @param identifier the identifier of the sent entity
		 */
		@JsonCreator
		public SentValue(String payload, String identifier) {
			this.payload = payload;
			this.identifier = identifier;
		}

		/**
		 * Returns the payload.
		 *
		 * @return the serialized payload
		 */
		public String getPayload() {
			return payload;
		}

		/**
		 * The identifier of the contained {@link SentEntity}.
		 *
		 * @return the identifier of the contained sent entity
		 */
		public String getIdentifier() {
			return identifier;
		}
	}
}
