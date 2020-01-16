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
}
