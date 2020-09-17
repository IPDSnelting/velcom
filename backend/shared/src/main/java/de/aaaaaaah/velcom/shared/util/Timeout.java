package de.aaaaaaah.velcom.shared.util;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A timeout is an action that activates after a certain duration if it is not cancelled. This
 * timeout allows for arbitrary actions to happen by returning a {@link CompletionStage} that the
 * user can attach any actions to.
 */
public class Timeout {

	private final Thread waitThread;
	private final CompletableFuture<Void> future;
	private volatile boolean cancelled;

	private Timeout(Thread waitThread, CompletableFuture<Void> future) {
		this.waitThread = waitThread;
		this.future = future;

		cancelled = false;
	}

	/**
	 * Create a timeout that activates after a certain amount of time.
	 *
	 * @param duration How long to wait until the timeout activates
	 * @return the timeout
	 */
	public static Timeout after(Duration duration) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		Thread waitThread = new Thread(() -> {
			try {
				Thread.sleep(duration.toMillis());
				future.complete(null);
			} catch (InterruptedException e) {
				future.cancel(true);
			}
		});

		return new Timeout(waitThread, future);
	}

	/**
	 * Start the timeout.
	 */
	public void start() {
		if (!cancelled) {
			waitThread.start();
		}
	}

	public CompletionStage<Void> getCompletionStage() {
		return future;
	}

	/**
	 * Cancel the timeout. Use this function instead of cancelling the {@link CompletionStage}
	 * returned by getCompletionStage directly. Can be called multiple times.
	 */
	public void cancel() {
		cancelled = true;
		waitThread.interrupt();
		future.cancel(true);
	}
}
