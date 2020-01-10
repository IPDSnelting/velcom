package de.aaaaaaah.designproto.runner.protocol;

import de.aaaaaaah.designproto.runner.entity.RunnerConfiguration;
import de.aaaaaaah.designproto.runner.exceptions.ConnectionException;
import de.aaaaaaah.designproto.runner.shared.protocol.HeartbeatHandler;
import de.aaaaaaah.designproto.runner.shared.protocol.HeartbeatHandler.HeartbeatWebsocket;
import de.aaaaaaah.designproto.runner.shared.protocol.SentEntity;
import de.aaaaaaah.designproto.runner.shared.protocol.exceptions.SerializationException;
import de.aaaaaaah.designproto.runner.shared.protocol.runnerbound.entities.ResetOrder;
import de.aaaaaaah.designproto.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.designproto.runner.shared.protocol.runnerbound.entities.UpdateBenchmarkRepoOrder;
import java.io.IOException;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The listener for thr websocket connection.
 */
public class WebsocketListener implements WebSocket.Listener, SocketConnectionManager,
	HeartbeatWebsocket {

	private final Object binaryLock;
	private final Object textLock;
	private RunnerConfiguration configuration;
	private WebSocket websocket;
	private Collection<ConnectionStateListener> stateListeners;
	private OutputStream outputStream;
	private Path outputFilePath;
	private StringBuilder textBuilder;
	private HeartbeatHandler heartbeatHandler;

	/**
	 * Creates a new websocket listener.
	 */
	public WebsocketListener() {
		this.binaryLock = new Object();
		this.textLock = new Object();
		this.textBuilder = new StringBuilder();
		this.stateListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

		addStateListener(new RestablishConnectionListener(this));
	}

	/**
	 * Sets the runner configuration.
	 *
	 * @param configuration the runner configuration
	 */
	public void setConfiguration(RunnerConfiguration configuration) {
		this.configuration = configuration;
	}

	//<editor-fold desc="Websocket API">
	@Override
	public void onOpen(WebSocket webSocket) {
		webSocket.request(1);

		disconnectImpl();

		this.websocket = webSocket;
		configuration.getRunnerStateMachine().onConnectionEstablished(configuration);
		stateListeners.forEach(it -> it.onStateChange(ConnectionState.CONNECTED));
		heartbeatHandler = new HeartbeatHandler(this);
	}

	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		webSocket.request(1);

		synchronized (textLock) {
			textBuilder.append(data);

			if (last) {
				String request = textBuilder.toString();
				System.out.println("Got: " + request);
				textBuilder = new StringBuilder();
				try {
					switch (configuration.getSerializer().peekType(request)) {
						case "RunnerWorkOrder":
							configuration.getRunnerStateMachine().onWorkArrived(
								configuration.getSerializer()
									.deserialize(request, RunnerWorkOrder.class),
								configuration
							);
							break;
						case "ResetOrder":
							configuration.getRunnerStateMachine().onResetRequested(
								configuration.getSerializer().deserialize(request, ResetOrder.class)
									.getReason(),
								configuration
							);
							break;
						case "UpdateBenchmarkRepoOrder":
							configuration.getRunnerStateMachine().onUpdateBenchmarkRepo(
								configuration.getSerializer()
									.deserialize(request, UpdateBenchmarkRepoOrder.class)
									.getCommitHash(),
								configuration
							);
							break;
					}
				} catch (SerializationException e) {
					System.err.println("Unknown message received!");
					e.printStackTrace();
				}
			}
		}

		return null;
	}

	@Override
	public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
		webSocket.request(1);
		synchronized (binaryLock) {
			try {
				handleWriteFile(data, last);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	private void handleWriteFile(ByteBuffer data, boolean last) throws IOException {
		if (outputStream == null) {
			outputFilePath = Files.createTempFile("runner", "");
			outputStream = Files.newOutputStream(outputFilePath);
		}
		byte[] buffer = new byte[data.remaining()];
		data.get(buffer);
		outputStream.write(buffer);

		if (last) {
			outputStream.close();
			outputStream = null;
			configuration.getRunnerStateMachine().onFileReceived(outputFilePath, configuration);
		}
	}

	@Override
	public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
		return WebSocket.Listener.super.onPing(webSocket, message);
	}

	@Override
	public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
		heartbeatHandler.onPong();
		return WebSocket.Listener.super.onPong(webSocket, message);
	}

	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		System.out.println("Closing...");
		disconnectImpl();
		return null;
	}

	@Override
	public void onError(WebSocket webSocket, Throwable error) {
		error.printStackTrace();
		disconnectImpl();
	}
	//</editor-fold>

	@Override
	public void sendEntity(SentEntity entity) {
		if (websocket == null) {
			throw new IllegalStateException("Not connected!");
		}
		websocket.sendText(configuration.getSerializer().serialize(entity), true);
	}

	@Override
	public void disconnect() {
		System.out.println("Disconnecting...");
		disconnectImpl();
	}

	@Override
	public void connect() throws ConnectionException {
		CompletableFuture<WebSocket> future = HttpClient.newHttpClient()
			.newWebSocketBuilder()
			.header("Authorization", configuration.getRunnerToken())
			.buildAsync(configuration.getServerUrl(), this);

		try {
			future.get(20, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new ConnectionException("Error connecting", e);
		}
	}

	@Override
	public boolean isConnected() {
		return websocket != null && !websocket.isInputClosed() && !websocket.isOutputClosed();
	}

	@Override
	public void addStateListener(ConnectionStateListener listener) {
		stateListeners.add(listener);
	}

	@Override
	public void removeStateListener(ConnectionStateListener listener) {
		stateListeners.remove(listener);
	}

	@Override
	public void onTimeoutDetected() {
		System.out.println("Timeout detected");
		disconnect();
	}

	@Override
	public boolean sendPing() {
		if (!isConnected()) {
			disconnectImpl();
			System.out.println("Not!");
			return false;
		}
		websocket.sendPing(ByteBuffer.allocate(Long.SIZE).putLong(System.currentTimeMillis()));
		return true;
	}

	private void disconnectImpl() {
		if (websocket != null) {
			websocket.sendClose(4000, "Client initiated close")
				.thenAccept(WebSocket::abort)
				.thenRun(
					() -> stateListeners.forEach(
						it -> it.onStateChange(ConnectionState.DISCONNECTED))
				);
		}
		if (heartbeatHandler != null) {
			heartbeatHandler.shutdown();
		}
		websocket = null;
	}
}
