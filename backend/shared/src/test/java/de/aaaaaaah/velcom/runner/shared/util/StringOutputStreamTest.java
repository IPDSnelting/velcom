package de.aaaaaaah.velcom.runner.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StringOutputStreamTest {

	private StringOutputStream outputStream;

	@BeforeEach
	void setUp() {
		outputStream = new StringOutputStream();
	}

	@Test
	void writeSingleChar() {
		char character = (char) ('A' + ThreadLocalRandom.current().nextInt('z' - 'A'));
		outputStream.write(character);
		assertEquals(
			character + "",
			outputStream.getString()
		);
	}

	@Test
	void writeString() throws IOException {
		String text = "Hello world there";
		outputStream.write(text.getBytes());
		assertEquals(
			text,
			outputStream.getString()
		);
	}

	@Test
	void writeStringMultipleCalls() throws IOException {
		String text = "Hello world there";
		outputStream.write(text.substring(0, 10).getBytes());
		outputStream.write(text.substring(10).getBytes());
		assertEquals(
			text,
			outputStream.getString()
		);
	}

	@Test
	void writeIsUTF8() throws IOException {
		String text = "Hello world thereäöüß Λλ†‡";
		outputStream.write(text.getBytes(StandardCharsets.UTF_8));
		assertEquals(
			text,
			outputStream.getString()
		);
	}

	@Test
	void writePartial() {
		String text = "Hello world";
		outputStream.write(text.getBytes(StandardCharsets.UTF_8), 0, 6);
		assertEquals(
			text.substring(0, 6),
			outputStream.getString()
		);
	}

}