package de.aaaaaaah.velcom.shared.protocol.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for tests that need a {@link Serializer}.
 */
public abstract class SerializerBasedTest {

	/**
	 * Use this to serialize or deserialize your data.
	 */
	protected Serializer serializer;

	/**
	 * Use this to create {@link com.fasterxml.jackson.databind.JsonNode}s whenever necessary.
	 */
	protected ObjectMapper objectMapper;

	@BeforeEach
	void setUpObjectMapper() {
		serializer = new Serializer();
		objectMapper = new ObjectMapper();
	}
}
