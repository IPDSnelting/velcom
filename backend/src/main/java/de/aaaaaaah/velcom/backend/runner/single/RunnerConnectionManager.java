package de.aaaaaaah.velcom.backend.runner.single;

import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Handles the connection to the runner.
 */
public interface RunnerConnectionManager {

	/**
	 * Disconnects the runner.
	 */
	void disconnect();

	/**
	 * Sends an entity.
	 *
	 * @param entity the entity to send
	 * @throws IOException if an error occurs
	 */
	void sendEntity(SentEntity entity) throws IOException;

	/**
	 * Returns a new output stream <em>that must be closed</em>, forwarding all written data to the
	 * runner.
	 *
	 * @return a new output stream that transfers data to the runner
	 */
	OutputStream createBinaryOutputStream();

	/**
	 * Adds a new connection state listener.
	 *
	 * @param listener the listener
	 */
	void addConnectionStateListener(ConnectionStateListener listener);

	/**
	 * Removes an existing connection state listener. Does nothing it the listener does not exist.
	 *
	 * @param listener the listener
	 */
	void removeConnectionStateListener(ConnectionStateListener listener);

	/**
	 * The state of the connection.
	 */
	enum ConnectionState {
		/**
		 * The runner is connected.
		 */
		CONNECTED,
		/**
		 * The runner is disconnected.
		 */
		DISCONNECTED
	}

	/**
	 * A listener for connection state changes.
	 */
	interface ConnectionStateListener {

		/**
		 * Called when the connection state changes.
		 *
		 * @param state the connection state
		 */
		void onStateChanged(ConnectionState state);
	}
}
