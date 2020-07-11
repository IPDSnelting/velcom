package de.aaaaaaah.velcom.runner.util;

import static org.assertj.core.api.Assertions.assertThat;

import de.aaaaaaah.velcom.shared.util.ExceptionHelper;
import org.junit.jupiter.api.Test;

class ExceptionHelperTest {

	@Test
	void containsThisClass() {
		String stackTrace = ExceptionHelper.getStackTrace(new Throwable());
		assertThat(stackTrace)
			.contains("ExceptionHelperTest")
			.contains("containsThisClass");
	}
}