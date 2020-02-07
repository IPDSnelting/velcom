package de.aaaaaaah.velcom.runner.protocol;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.runner.exceptions.ConnectionException;
import de.aaaaaaah.velcom.runner.protocol.SocketConnectionManager.ConnectionState;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReestablishConnectionListenerTest {

	private ReestablishConnectionListener listener;
	private SocketConnectionManager socketConnectionManager;

	@BeforeEach
	void setUp() {
		socketConnectionManager = mock(SocketConnectionManager.class);
		listener = new ReestablishConnectionListener(socketConnectionManager);
	}

	@AfterEach
	void tearDown() {
		listener.stopReconnecting();
	}


	@Test
	void triesToReconnect() throws InterruptedException {
		listener.onStateChange(ConnectionState.DISCONNECTED);
		Thread.sleep(100);
		verify(socketConnectionManager).connect();
	}

	@Test
	void triesToReconnectTwice() throws InterruptedException {
		listener.onStateChange(ConnectionState.DISCONNECTED);
		Thread.sleep(100);
		listener.onStateChange(ConnectionState.DISCONNECTED);
		Thread.sleep(1200);
		verify(socketConnectionManager, times(2)).connect();
	}

	@Test
	void doesNotReconnectOnConnected() throws InterruptedException {
		listener.onStateChange(ConnectionState.CONNECTED);
		Thread.sleep(100);
		verify(socketConnectionManager, times(0)).connect();
	}

	@Test
	void survivesConnectException() throws InterruptedException {
		doThrow(ConnectionException.class).when(socketConnectionManager).connect();

		listener.onStateChange(ConnectionState.DISCONNECTED);
		Thread.sleep(2200);
		verify(socketConnectionManager, times(2)).connect();
	}

	@Test
	void doesNotCallConnectTooOftenWhenConnectFails() throws InterruptedException {
		doThrow(ConnectionException.class).when(socketConnectionManager).connect();

		listener.onStateChange(ConnectionState.DISCONNECTED);
		listener.onStateChange(ConnectionState.DISCONNECTED);
		listener.onStateChange(ConnectionState.DISCONNECTED);
		listener.onStateChange(ConnectionState.DISCONNECTED);
		Thread.sleep(2000);
		verify(socketConnectionManager, times(1)).connect();
	}

	@Test
	void doesNotCallConnectTooOftenWhenConnectSucceeds() throws InterruptedException {
		AtomicBoolean connected = new AtomicBoolean(false);
		when(socketConnectionManager.isConnected())
			.thenAnswer(invocation -> connected.getAndSet(true));

		listener.onStateChange(ConnectionState.DISCONNECTED);
		listener.onStateChange(ConnectionState.DISCONNECTED);
		listener.onStateChange(ConnectionState.DISCONNECTED);
		listener.onStateChange(ConnectionState.DISCONNECTED);

		Thread.sleep(100);
		verify(socketConnectionManager, times(1)).connect();
	}
}