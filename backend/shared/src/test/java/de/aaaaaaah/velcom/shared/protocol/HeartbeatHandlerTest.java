package de.aaaaaaah.velcom.shared.protocol;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.shared.protocol.HeartbeatHandler.HeartbeatWebsocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HeartbeatHandlerTest {

	private HeartbeatHandler heartbeatHandler;
	private HeartbeatWebsocket heartbeatWebsocket;

	@BeforeEach
	void setUp() {
		heartbeatWebsocket = mock(HeartbeatWebsocket.class);
		heartbeatHandler = new HeartbeatHandler(heartbeatWebsocket, 100);

		when(heartbeatWebsocket.sendPing()).thenReturn(true);
	}

	@AfterEach
	void tearDown() {
		heartbeatHandler.shutdown();
	}


	@Test
	void testSendsPing() throws InterruptedException {
		Thread.sleep(200);

		verify(heartbeatWebsocket, atLeastOnce()).onTimeoutDetected();
	}

	@Test
	void onPongDelaysReaping() throws InterruptedException {
		for (int i = 0; i < 20; i++) {
			Thread.sleep(20);
			heartbeatHandler.onPong();
		}

		verify(heartbeatWebsocket, never()).onTimeoutDetected();
	}

	@Test
	void doesNotReapIfPingFails() throws InterruptedException {
		when(heartbeatWebsocket.sendPing()).thenReturn(false);
		Thread.sleep(200);

		verify(heartbeatWebsocket, never()).onTimeoutDetected();
	}
}