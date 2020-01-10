package de.aaaaaaah.designproto.runner.protocol;

import de.aaaaaaah.designproto.runner.exceptions.ConnectionException;
import de.aaaaaaah.designproto.runner.shared.protocol.SentEntity;
import java.io.IOException;

/**
 * Manages the connection to the server.
 */
public interface SocketConnectionManager {

	/**
	 * Sends an entity.
	 *
	 * @param entity the entity to send
	 * @throws IOException if sending failed
	 */
	void sendEntity(SentEntity entity) throws IOException;

	/**
	 * Disconnects from the server.
	 */
	void disconnect();

	/**
	 * Establishes a connection. This method is <em>blocking</em>.
	 *
	 * @throws ConnectionException if connecting failed
	 */
	void connect() throws ConnectionException;

	/**
	 * Returns whether the connection is currently open.
	 *
	 * @return true if the connection is currently open
	 */
	boolean isConnected();

	/**
	 * Adds a new connection state listener.
	 *
	 * @param listener the listener
	 */
	void addStateListener(ConnectionStateListener listener);

	/**
	 * Removes a given connection state listener. Does nothing it the listener does not exist.
	 *
	 * @param listener the listener
	 */
	void removeStateListener(ConnectionStateListener listener);

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
	 * A listener that is notified when the connection state changes.
	 */
	interface ConnectionStateListener {

		/**
		 * Called when the state changes.
		 *
		 * @param newState the new state
		 */
		void onStateChange(ConnectionState newState);
	}
}
