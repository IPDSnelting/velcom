package de.aaaaaaah.velcom.runner.shared.protocol;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends and receives heartbeats, calling a timeout handler if appropriate.
 */
public class HeartbeatHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatHandler.class);

	private static final int TIMEOUT_SECONDS = 20;

	private AtomicReference<Instant> lastTime;
	private volatile boolean run;

	/**
	 * Creates a new heartbeat handler for the given socket.
	 *
	 * @param socketConnection the socket to montor
	 */
	public HeartbeatHandler(HeartbeatWebsocket socketConnection) {
		this.lastTime = new AtomicReference<>(Instant.now());
		this.run = true;

		Thread thread = new Thread(() -> {
			while (run) {
				try {
					Thread.sleep(TIMEOUT_SECONDS / 2 * 1000);
				} catch (InterruptedException ignored) {
				}
				if (!run) {
					break;
				}
				LOGGER.debug("Sending ping!");
				if (!socketConnection.sendPing()) {
					LOGGER.warn("Ping to runner/server failed!");
					continue;
				}
				long secondsSinceLastPing = Duration.between(lastTime.get(), Instant.now())
					.getSeconds();
				if (secondsSinceLastPing > TIMEOUT_SECONDS) {
					socketConnection.onTimeoutDetected();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Should be called when a pong was received.
	 */
	public void onPong() {
		LOGGER.debug("Got pong!");
		lastTime.set(Instant.now());
	}

	/**
	 * Shuts down this manager.
	 */
	public void shutdown() {
		run = false;
	}

	/**
	 * An abstraction for a websocket that is usable for this manager.
	 */
	public interface HeartbeatWebsocket {

		/**
		 * Called when the manager detects a timeout.
		 */
		void onTimeoutDetected();

		/**
		 * Sends a ping message. Calls {@link #onPong()} if the resulting pong is detected.
		 *
		 * @return true if the ping was sent successfully
		 */
		boolean sendPing();
	}
}
