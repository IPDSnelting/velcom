package de.aaaaaaah.velcom.backend.runner.single.protocol;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.backend.runner.single.ServerRunnerStateMachine;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.UpdateBenchmarkRepoOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Serializer;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.SimpleJsonSerializer;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.WorkReceived;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.api.extensions.Frame.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunnerServerWebsocketListenerTest {

	private RunnerServerWebsocketListener listener;
	private Serializer serializer;
	private ServerRunnerStateMachine stateMachine;
	private ActiveRunnerInformation runnerInformation;
	private Session session;
	private RemoteEndpoint remote;

	@BeforeEach
	void setUp() {
		serializer = new SimpleJsonSerializer();
		runnerInformation = mock(ActiveRunnerInformation.class);
		stateMachine = mock(ServerRunnerStateMachine.class);
		session = mock(Session.class);
		remote = mock(RemoteEndpoint.class);

		when(runnerInformation.getRunnerStateMachine()).thenReturn(stateMachine);
		when(session.getRemote()).thenReturn(remote);

		listener = new RunnerServerWebsocketListener(serializer);
		listener.setRunnerInformation(runnerInformation);
		listener.onWebSocketConnect(session);
	}

	@AfterEach
	void tearDown() {
		listener.disconnect();
	}

	@Test
	void forwardsRunnerInformation() {
		verifyForwardedEntity(new RunnerInformation(
			"dsd", "OS", 20, 10, RunnerStatusEnum.IDLE, "dfsds"
		));
	}

	@Test
	void forwardsWorkReceived() {
		verifyForwardedEntity(new WorkReceived(
			new RunnerWorkOrder(UUID.randomUUID(), "ds")
		));
	}

	@Test
	void forwardsResults() {
		verifyForwardedEntity(new BenchmarkResults(
			new RunnerWorkOrder(UUID.randomUUID(), "hash"),
			"error", Instant.now(), Instant.now()
		));
	}

	@Test
	void marksLastMessageTime() {
		Frame frame = mock(Frame.class);
		when(frame.getType()).thenReturn(Type.PONG);
		listener.onWebSocketFrame(frame);

		verify(runnerInformation).setLastReceivedMessage(any(Instant.class));
	}

	@Test
	void disconnectForwardsCodes() {
		when(session.isOpen()).thenReturn(true);
		listener.disconnect(200, "Test");

		verify(session).close(200, "Test");
	}

	@Test
	void sendEntity() throws IOException {
		UpdateBenchmarkRepoOrder entity = new UpdateBenchmarkRepoOrder("hash");
		listener.sendEntity(entity);

		verify(remote).sendString(serializer.serialize(entity));
	}

	@Test
	void disconnectsOnError() {
		when(session.isOpen()).thenReturn(true);
		listener.onWebSocketError(new RuntimeException());

		verify(session).close(anyInt(), anyString());
	}

	@Test
	void setsDisconnectedStatusOnClose() {
		listener.onWebSocketClose(200, "Hey");

		verify(runnerInformation).setDisconnected(200);
	}

	@Test
	void disconnectsOnBinaryReceived() {
		listener.onWebSocketBinary(new byte[1], 0, 1);

		verify(runnerInformation).setDisconnected(anyInt());
	}

	private void verifyForwardedEntity(SentEntity entity) {
		String serialized = serializer.serialize(entity);
		String type = serializer.peekType(serialized);

		listener.onWebSocketText(serialized);

		verify(stateMachine).onMessageReceived(type, entity);
	}


}