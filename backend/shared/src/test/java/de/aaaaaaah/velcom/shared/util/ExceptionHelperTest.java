package de.aaaaaaah.velcom.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExceptionHelperTest {

	@Test
	void containsThisClass() {
		String stackTrace = ExceptionHelper.getStackTrace(new Throwable());
		assertThat(stackTrace)
			.contains("ExceptionHelperTest")
			.contains("containsThisClass");
	}

	@Test
	void containsNestedStacktrace() {
		RuntimeException inner = new RuntimeException("hello there");
		RuntimeException outer = new RuntimeException(inner);

		String stackTrace = ExceptionHelper.getStackTrace(outer);
		assertThat(stackTrace)
			.contains("ExceptionHelperTest")
			.contains("containsNestedStacktrace")
			.contains("hello there");
	}

	@Test
	void containsSuppressedStacktrace() {
		RuntimeException inner = new RuntimeException("hello there");
		RuntimeException outer = new RuntimeException();
		outer.addSuppressed(inner);

		String stackTrace = ExceptionHelper.getStackTrace(outer);
		assertThat(stackTrace)
			.contains("ExceptionHelperTest")
			.contains("containsSuppressedStacktrace")
			.contains("hello there");
	}
}