package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

public abstract class SerializingTest {

	protected ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper()
			.registerModule(new ParameterNamesModule())
			.registerModule(new Jdk8Module())
			.registerModule(new JavaTimeModule())
			.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
			.setSerializationInclusion(Include.NON_NULL)
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}

	protected void serializedEquals(Object object, String json) throws JsonProcessingException {
		JsonNode objectTree = objectMapper.readTree(objectMapper.writeValueAsString(object));
		JsonNode jsonTree = objectMapper.readTree(json);
		Assertions.assertThat(objectTree).isEqualTo(jsonTree);
	}
}
