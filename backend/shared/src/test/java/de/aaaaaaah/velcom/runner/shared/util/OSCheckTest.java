package de.aaaaaaah.velcom.runner.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OSCheckTest {

	@Test
	void returnsCorrectValue() {
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			assertThat(OSCheck.isStupidWindows()).isTrue();
		} else {
			assertThat(OSCheck.isStupidWindows()).isFalse();
		}
	}
}