package de.aaaaaaah.velcom.shared.protocol.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class SerializerTest extends SerializerBasedTest {

	@Test
	void deserializeFailReturnsEmpty() {
		assertThat(serializer.deserialize("hello", SerializerBasedTest.class)).isEmpty();
	}
}
