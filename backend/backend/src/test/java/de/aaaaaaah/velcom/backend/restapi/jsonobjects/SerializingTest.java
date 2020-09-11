package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

public abstract class SerializingTest {

	protected ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		// This mapper should be configured the same as the one in ServerMain.java
		objectMapper = new ObjectMapper()
			.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
			.setSerializationInclusion(Include.NON_NULL);
	}

	protected void serializedEquals(Object object, String json) throws JsonProcessingException {
		JsonNode objectTree = objectMapper.readTree(objectMapper.writeValueAsString(object));
		JsonNode jsonTree = objectMapper.readTree(json);
		Assertions.assertThat(objectTree).isEqualTo(jsonTree);
	}
}
