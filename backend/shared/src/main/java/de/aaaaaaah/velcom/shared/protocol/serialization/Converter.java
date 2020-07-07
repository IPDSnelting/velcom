package de.aaaaaaah.velcom.shared.protocol.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.util.Optional;

/**
 * Serialize and deserialize client- and serverbound packets. This class is mainly a wrapper around
 * an {@link ObjectMapper} so it is easier to use and doesn't have to be configured every time.
 */
public class Converter {

	private final ObjectMapper objectMapper;

	public Converter() {
		objectMapper = new ObjectMapper()
			.registerModule(new ParameterNamesModule())
			.registerModule(new Jdk8Module())
			.registerModule(new JavaTimeModule())
			.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}

	/**
	 * Serialize an object to JSON.
	 *
	 * @param value the object to serialize
	 * @return the JSON representation of that object, if successful
	 */
	public Optional<String> serialize(Object value) {
		try {
			return Optional.of(objectMapper.writeValueAsString(value));
		} catch (JsonProcessingException e) {
			return Optional.empty();
		}
	}

	/**
	 * Serialize an object to a JSON tree.
	 *
	 * @param value the object to serialize
	 * @return the JSON representation of that object
	 */
	public JsonNode serializeTree(Object value) {
		return objectMapper.valueToTree(value);
	}

	/**
	 * Deserialize an object from a JSON string.
	 *
	 * @param input the string to read from
	 * @param type the class of the type to deserialize into
	 * @param <T> the type to deserialize into
	 * @return the deserialized object, if successful
	 */
	public <T> Optional<T> deserialize(String input, Class<T> type) {
		try {
			return Optional.of(objectMapper.readValue(input, type));
		} catch (JsonProcessingException e) {
			return Optional.empty();
		}
	}

	/**
	 * Deserialize an object from a JSON tree.
	 *
	 * @param input the input tree to use
	 * @param type the class of the type to deserialize into
	 * @param <T> the type to deserialize into
	 * @return the deserialized object, if successful
	 */
	public <T> Optional<T> deserialize(JsonNode input, Class<T> type) {
		try {
			return Optional.of(objectMapper.treeToValue(input, type));
		} catch (JsonProcessingException e) {
			return Optional.empty();
		}
	}

}
