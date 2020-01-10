package de.aaaaaaah.velcom.backend.runner.single.protocol;

import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Serializer;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.eclipse.jetty.websocket.api.Session;

/**
 * A small helper for websocket protocol sending.
 */
public class ProtocolHelper {

	private static final int DEFAULT_BUFFER_SIZE = 10 * 1024; // 10 MB

	/**
	 * Sends an object to the runner.
	 *
	 * @param session the session to use for sending
	 * @param object the object to send
	 * @param serializer the serializer to use
	 * @throws IOException if there is a communication error
	 */
	public static void sendObject(Session session, SentEntity object, Serializer serializer)
		throws IOException {
		session.getRemote().sendString(serializer.serialize(object));
	}

	/**
	 * Streams binary data to the runner.
	 *
	 * @param session the session to use
	 * @return an output stream that streams written data to the runner
	 */
	public static OutputStream createBinaryOutputStream(Session session) {
		return new OutputStream() {
			private ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);

			@Override
			public void write(int b) throws IOException {
				if (!buffer.hasRemaining()) {
					buffer.rewind();
					session.getRemote().sendPartialBytes(buffer, false);
					buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
				}
				buffer.put((byte) b);
			}

			@Override
			public void close() throws IOException {
				buffer.limit(buffer.position());
				buffer.rewind();
				session.getRemote().sendPartialBytes(buffer, true);
			}
		};
	}

}
