package de.aaaaaaah.velcom.runner.shared.protocol.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.exceptions.SerializationException;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SimpleJsonSerializerTest {

	private SimpleJsonSerializer serializer;

	@BeforeEach
	void setUp() {
		serializer = new SimpleJsonSerializer();
	}

	@Test
	void roundtrip() {
		RunnerWorkOrder input = new RunnerWorkOrder(UUID.randomUUID(), "hey");
		String serialized = serializer.serialize(input);
		System.out.println(serialized);

		assertThat(serializer.deserialize(serialized, RunnerWorkOrder.class))
			.isEqualTo(input);
	}

	@Test
	void peekType() {
		RunnerWorkOrder input = new RunnerWorkOrder(UUID.randomUUID(), "hey");
		String serialized = serializer.serialize(input);

		assertThat(serializer.peekType(serialized))
			.isEqualTo(RunnerWorkOrder.class.getSimpleName());
	}

	@ParameterizedTest
	@CsvSource(value = {
		"de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder | {",
		"de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder | {\"payload\":\"hey\",\"identifier\":\"RunnerWorkOrder\"}\n"
	}, delimiter = '|')
	void deserializeInvalid(String className, String input) {
		assertThatThrownBy(() -> {
			@SuppressWarnings("unchecked")
			Class<? extends SentEntity> aClass = (Class<? extends SentEntity>)
				Class.forName(className);
			System.out.println("Got: " + serializer.deserialize(input, aClass));
		})
			.isInstanceOf(SerializationException.class);
	}

}
