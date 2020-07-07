package de.aaaaaaah.velcom.runner.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.protocol.SocketConnectionManager.ConnectionState;
import de.aaaaaaah.velcom.runner.protocol.SocketConnectionManager.ConnectionStateListener;
import de.aaaaaaah.velcom.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.shared.protocol.StatusCodeMappings;
import de.aaaaaaah.velcom.shared.protocol.runnerbound.entities.ResetOrder;
import de.aaaaaaah.velcom.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.shared.protocol.runnerbound.entities.UpdateBenchmarkRepoOrder;
import de.aaaaaaah.velcom.shared.protocol.serialization.SimpleJsonSerializer;
import de.aaaaaaah.velcom.runner.state.RunnerStateMachine;
import java.io.IOException;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WebsocketListenerTest {

	private WebsocketListener websocketListener;
	private RunnerConfiguration configuration;
	private RunnerStateMachine stateMachine;
	private SimpleJsonSerializer serializer;

	@BeforeEach
	void setUp() {
		websocketListener = new WebsocketListener();
		serializer = new SimpleJsonSerializer();
		configuration = mock(RunnerConfiguration.class);
		stateMachine = mock(RunnerStateMachine.class);

		when(configuration.getRunnerStateMachine()).thenReturn(stateMachine);
		when(configuration.getSerializer()).thenReturn(serializer);

		websocketListener.setConfiguration(configuration);
	}

	@Test
	void onTextDelegatesToReset() {
		String data = serialize(new ResetOrder("Reset!"));
		WebSocket webSocket = mock(WebSocket.class);
		websocketListener.onText(webSocket, data, true);

		verify(stateMachine).onResetRequested(eq("Reset!"), eq(configuration));
	}

	@Test
	void onTextDelegatesToWorkArrived() {
		RunnerWorkOrder workOrder = new RunnerWorkOrder(UUID.randomUUID(), "hash");
		String data = serialize(workOrder);
		WebSocket webSocket = mock(WebSocket.class);
		websocketListener.onText(webSocket, data, true);

		verify(stateMachine).onWorkArrived(eq(workOrder), eq(configuration));
	}

	@Test
	void onTextDelegatesToUpdateBenchrepo() {
		String data = serialize(new UpdateBenchmarkRepoOrder("hash"));
		WebSocket webSocket = mock(WebSocket.class);
		websocketListener.onText(webSocket, data, true);

		verify(stateMachine).onUpdateBenchmarkRepo(eq("hash"), eq(configuration));
	}

	@Test
	void onTextIgnoresOtherPackets() {
		WebSocket webSocket = mock(WebSocket.class);

		assertThat(websocketListener.onText(webSocket, "Test", true)).isNull();
	}

	@Test
	void onTextCanHandlePartialData() {
		String commitHash = "hash long enough to pad 20";
		String data = serialize(new UpdateBenchmarkRepoOrder(commitHash));
		WebSocket webSocket = mock(WebSocket.class);
		websocketListener.onText(webSocket, data.substring(0, 10), false);
		websocketListener.onText(webSocket, data.substring(10, 20), false);
		websocketListener.onText(webSocket, data.substring(20), true);

		verify(stateMachine).onUpdateBenchmarkRepo(eq(commitHash), eq(configuration));
	}

	@Test
	void onBinaryReceiveFile() throws IOException {
		WebSocket webSocket = mock(WebSocket.class);
		websocketListener.onBinary(webSocket, ByteBuffer.wrap("hello".getBytes()), true);

		ArgumentCaptor<Path> captor = ArgumentCaptor.forClass(Path.class);
		verify(stateMachine).onFileReceived(captor.capture(), eq(configuration));

		assertThat(Files.readString(captor.getValue())).isEqualTo("hello");

		Files.deleteIfExists(captor.getValue());
	}

	@Test
	void onBinaryReceiveFileCanHandleChunks() throws IOException {
		WebSocket webSocket = mock(WebSocket.class);
		String text = "hello world how are you today? $0 chars are hard!";

		ByteBuffer buffer = ByteBuffer.wrap(text.getBytes());
		ByteBuffer first = buffer.slice().limit(20);
		ByteBuffer second = buffer.slice().position(20).limit(40);
		ByteBuffer third = buffer.slice().position(40);
		websocketListener.onBinary(webSocket, first, false);
		websocketListener.onBinary(webSocket, second, false);
		websocketListener.onBinary(webSocket, third, true);

		ArgumentCaptor<Path> captor = ArgumentCaptor.forClass(Path.class);
		verify(stateMachine).onFileReceived(captor.capture(), eq(configuration));

		assertThat(Files.readString(captor.getValue())).isEqualTo(text);

		Files.deleteIfExists(captor.getValue());
	}

	@Test
	void sendEntitySerializesIt() {
		WebSocket webSocket = mock(WebSocket.class);
		ResetOrder order = new ResetOrder("Hey");

		websocketListener.onOpen(webSocket);
		websocketListener.sendEntity(order);

		verify(webSocket).sendText(serialize(order), true);
	}

	@Test
	void sendPingSendsPing() {
		WebSocket socket = mockOpenSocket();
		websocketListener.onOpen(socket);

		websocketListener.sendPing();

		verify(socket, atLeastOnce()).sendPing(any());
	}

	@Test
	void disconnectSendsClose() {
		WebSocket socket = mockOpenSocket();
		websocketListener.onOpen(socket);

		when(socket.sendClose(anyInt(), anyString()))
			.thenAnswer(invocation -> CompletableFuture.completedStage(invocation.getMock()));

		websocketListener.disconnect();

		verify(socket, atLeastOnce())
			.sendClose(eq(StatusCodeMappings.CLIENT_ORDERLY_DISCONNECT), anyString());
		verify(socket, atLeastOnce()).abort();
	}

	@Test
	void openCallsListeners() {
		WebSocket socket = mockOpenSocket();
		ConnectionStateListener listener = mock(ConnectionStateListener.class);
		websocketListener.addStateListener(listener);

		websocketListener.onOpen(socket);

		verify(listener).onStateChange(ConnectionState.CONNECTED);
	}

	@Test
	void errorCallsListeners() {
		WebSocket socket = mockOpenSocket();
		ConnectionStateListener listener = mock(ConnectionStateListener.class);
		websocketListener.addStateListener(listener);

		websocketListener.onError(socket, new RuntimeException());
		verify(listener).onStateChange(ConnectionState.DISCONNECTED);
	}

	@Test
	void closeCallsListeners() {
		WebSocket socket = mockOpenSocket();
		ConnectionStateListener listener = mock(ConnectionStateListener.class);
		websocketListener.addStateListener(listener);

		websocketListener.onClose(socket, 20, "hey");
		verify(listener).onStateChange(ConnectionState.DISCONNECTED);
	}

	@Test
	void disconnectCallsListeners() {
		ConnectionStateListener listener = mock(ConnectionStateListener.class);
		websocketListener.addStateListener(listener);

		websocketListener.disconnect();
		verify(listener).onStateChange(ConnectionState.DISCONNECTED);
	}

	@Test
	void disconnectOnTimeout() {
		ConnectionStateListener listener = mock(ConnectionStateListener.class);
		websocketListener.addStateListener(listener);

		websocketListener.onTimeoutDetected();
		verify(listener).onStateChange(ConnectionState.DISCONNECTED);
	}

	@Test
	void unregisterListenersWorks() {
		ConnectionStateListener listener = mock(ConnectionStateListener.class);
		websocketListener.addStateListener(listener);
		websocketListener.removeStateListener(listener);

		websocketListener.onTimeoutDetected();
		verify(listener, times(0)).onStateChange(ConnectionState.DISCONNECTED);
	}

	private WebSocket mockOpenSocket() {
		WebSocket socket = mock(WebSocket.class);
		when(socket.isInputClosed()).thenReturn(false);
		when(socket.isOutputClosed()).thenReturn(false);
		return socket;
	}

	private String serialize(SentEntity entity) {
		return serializer.serialize(entity);
	}
}