package de.aaaaaaah.velcom.shared.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeoutTest {

	private static final Duration TIMEOUT_DURATION = Duration.ofMillis(100);

	private volatile boolean completed;

	@BeforeEach
	void setUp() {
		completed = false;
	}

	private void handler(Void aVoid) {
		completed = true;
	}

	@Test
	void timeOutProperly() throws InterruptedException {
		Timeout timeout = Timeout.after(TIMEOUT_DURATION);
		timeout.getCompletionStage().thenAccept(this::handler);
		timeout.start();

		Thread.sleep(TIMEOUT_DURATION.toMillis() * 2);
		assertTrue(completed);
	}

	@Test
	void cancelWhileRunning() throws InterruptedException {
		Timeout timeout = Timeout.after(TIMEOUT_DURATION);
		timeout.getCompletionStage().thenAccept(this::handler);
		timeout.start();

		Thread.sleep(TIMEOUT_DURATION.toMillis() / 2);
		timeout.cancel();

		Thread.sleep(TIMEOUT_DURATION.toMillis() * 2);
		assertFalse(completed);
	}

	@Test
	void cancelMultipleTimes() throws InterruptedException {
		Timeout timeout = Timeout.after(TIMEOUT_DURATION);
		timeout.getCompletionStage().thenAccept(this::handler);
		timeout.start();

		Thread.sleep(TIMEOUT_DURATION.toMillis() / 2);
		timeout.cancel();
		timeout.cancel();
		timeout.cancel();
		timeout.cancel();
		timeout.cancel();

		Thread.sleep(TIMEOUT_DURATION.toMillis() * 2);
		assertFalse(completed);
	}
}
