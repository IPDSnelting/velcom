package de.aaaaaaah.velcom.backend.runner.single.protocol;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Serializer;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.SimpleJsonSerializer;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProtocolHelperTest {

	private Serializer serializer;
	private Session session;
	private RemoteEndpoint remote;

	@BeforeEach
	void setUp() {
		serializer = new SimpleJsonSerializer();
		session = mock(Session.class);
		remote = mock(RemoteEndpoint.class);

		when(session.getRemote()).thenReturn(remote);
	}

	@Test
	void testSendingText() throws IOException {
		RunnerWorkOrder order = new RunnerWorkOrder(UUID.randomUUID(), "hey");
		ProtocolHelper.sendObject(
			session,
			order,
			serializer
		);

		verify(remote).sendString(serializer.serialize(order));
	}

	@Test
	void sendSmallInOneBatch() throws IOException {
		OutputStream outputStream = ProtocolHelper.createBinaryOutputStream(session);

		byte[] payload = "hello".getBytes();
		outputStream.write(payload);
		outputStream.close();

		verify(remote).sendPartialBytes(ByteBuffer.wrap(payload), true);
	}

	@Test
	void chunkOutput() throws IOException {
		OutputStream outputStream = ProtocolHelper.createBinaryOutputStream(session);

		byte[] payload = "hello".repeat(1_000).getBytes();
		outputStream.write(payload);
		outputStream.close();

		verify(remote, atLeast(2)).sendPartialBytes(any(ByteBuffer.class), eq(false));
		// Sends close frame
		verify(remote, times(1)).sendPartialBytes(any(ByteBuffer.class), eq(true));
	}

	@Test
	void outputStreamClosingIsIgnored() throws IOException {
		OutputStream outputStream = ProtocolHelper.createBinaryOutputStream(session);

		byte[] payload = "hello".getBytes();
		outputStream.write(payload);
		outputStream.close();
		outputStream.close();
		outputStream.close();

		verify(remote, times(0)).sendPartialBytes(any(ByteBuffer.class), eq(false));

		// Sends ONE close frame
		verify(remote, times(1)).sendPartialBytes(any(ByteBuffer.class), eq(true));
	}
}