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

	private static final int DEFAULT_TIMEOUT_SECONDS = 20;

	private final AtomicReference<Instant> lastTime;
	private volatile boolean run;

	/**
	 * Creates a new heartbeat handler for the given socket.
	 *
	 * <p>
	 * Uses the default timeout of <em>currently</em> 20 seconds.
	 *
	 * @param socketConnection the socket to monitor
	 */
	public HeartbeatHandler(HeartbeatWebsocket socketConnection) {
		this(socketConnection, DEFAULT_TIMEOUT_SECONDS);
	}

	/**
	 * Creates a new heartbeat handler for the given socket.
	 *
	 * @param socketConnection the socket to monitor
	 * @param timeoutMillis the duration in millis after which the connection is dropped
	 */
	public HeartbeatHandler(HeartbeatWebsocket socketConnection, int timeoutMillis) {
		this.lastTime = new AtomicReference<>(Instant.now());
		this.run = true;

		Thread thread = new Thread(() -> {
			while (run) {
				try {
					Thread.sleep(timeoutMillis / 2);
				} catch (InterruptedException ignored) {
				}
				if (!run) {
					break;
				}
				LOGGER.debug("Sending ping!");

				// If the ping was successful, it was *SENT* successfully.
				// This does not mean we got an answer.
				// If sending the ping failed already, that is a deeper problem the socket should
				// habdle - and we do not try to!
				if (!socketConnection.sendPing()) {
					LOGGER.warn("Ping to runner/server failed!");
					continue;
				}
				long millisSinceLastPing = Duration.between(lastTime.get(), Instant.now())
					.toMillis();
				if (millisSinceLastPing > timeoutMillis) {
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
