package de.aaaaaaah.velcom.backend.runner.single;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.eclipse.jetty.websocket.api.Session;

/**
 * A binary output stream streaming data to the runner.
 */
class TeleBinaryOutputStream extends OutputStream {

	private static final int DEFAULT_BUFFER_SIZE = 1024; // 1 MB

	private boolean closed;
	private ByteBuffer buffer;
	private final Session session;

	public TeleBinaryOutputStream(Session session) {
		this.session = session;
		this.buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
	}

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
		if (closed) {
			return;
		}
		buffer.limit(buffer.position());
		buffer.rewind();
		session.getRemote().sendPartialBytes(buffer, true);
		closed = true;
	}

}
