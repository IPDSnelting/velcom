package de.aaaaaaah.velcom.shared.protocol.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.BeforeEach;

public abstract class EntityTest {

	protected ObjectMapper objectMapper;

	@BeforeEach
	void setUpObjectMapper() {
		objectMapper = new ObjectMapper()
			.registerModule(new ParameterNamesModule())
			.registerModule(new Jdk8Module())
			.registerModule(new JavaTimeModule())
			.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
			.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}
}
