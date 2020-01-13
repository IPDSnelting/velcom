package de.aaaaaaah.velcom.runner.protocol;

import de.aaaaaaah.velcom.runner.exceptions.ConnectionException;
import de.aaaaaaah.velcom.runner.protocol.SocketConnectionManager.ConnectionState;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tries to reestablish a connection if the connection is lost.
 */
public class ReestablishConnectionListener implements
	SocketConnectionManager.ConnectionStateListener {

	private ScheduledExecutorService scheduler;
	private SocketConnectionManager connectionManager;
	private int currentBackoffTry;
	private volatile boolean stop;
	private ScheduledFuture<?> scheduledFuture;
	private Instant nextCheckTime = Instant.now();

	/**
	 * Creates a new reconnect listener for a given connection manager.
	 *
	 * @param connectionManager the connection manager
	 */
	public ReestablishConnectionListener(SocketConnectionManager connectionManager) {
		this.connectionManager = connectionManager;

		this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			return thread;
		});

		this.currentBackoffTry = 0;
		this.stop = false;
	}

	@Override
	public void onStateChange(ConnectionState newState) {
		if (newState == ConnectionState.DISCONNECTED) {
			// Freshly disconnected
			nextCheckTime = Instant.now();
			currentBackoffTry = 0;
			scheduledFuture = scheduler.schedule(() -> this.reconnect(0), 0, TimeUnit.SECONDS);
		} else {
			currentBackoffTry = 0;
			scheduledFuture = null;
		}
	}

	private void reconnect(int expectedBackoffCount) {
		if (connectionManager.isConnected()) {
			return;
		}
		if (stop) {
			return;
		}
		// Guards against too many state changes (should not happen if the connection manager is nice)
		// Another invocation came before us
		if (currentBackoffTry != expectedBackoffCount) {
			return;
		}
		// We are an earlier leftover invocation
		if (Instant.now().isBefore(nextCheckTime)) {
			return;
		}
		try {
			connectionManager.connect();
		} catch (ConnectionException e) {
			currentBackoffTry++;
			nextCheckTime = Instant.now().plusSeconds(getSleepTime());

			long sleepTime = getSleepTime();
			System.out.println("Trying to reconnect in " + sleepTime + "s");
			scheduledFuture = scheduler.schedule(() -> this.reconnect(currentBackoffTry), sleepTime,
				TimeUnit.SECONDS);
		}
	}

	/**
	 * @return the time in seconds to sleep before reconnecting
	 */
	private long getSleepTime() {
		// TODO: 14.01.20 One minute too low? 
		return Math.min((long) Math.exp(currentBackoffTry), TimeUnit.MINUTES.toSeconds(1));
	}

	/**
	 * Stops trying to reconnect. You can no longer use this listener after a call to this method.
	 */
	public void stopReconnecting() {
		stop = true;
		if (scheduledFuture != null) {
			scheduledFuture.cancel(true);
		}
	}

	/**
	 * Schedules a reconnect.
	 */
	public void scheduleReconnect() {
		onStateChange(ConnectionState.DISCONNECTED);
	}
}
