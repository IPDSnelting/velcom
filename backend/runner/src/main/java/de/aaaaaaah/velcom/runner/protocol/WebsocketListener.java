package de.aaaaaaah.velcom.runner.protocol;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.exceptions.ConnectionException;
import de.aaaaaaah.velcom.runner.exceptions.HandshakeFailureException;
import de.aaaaaaah.velcom.runner.shared.protocol.HeartbeatHandler;
import de.aaaaaaah.velcom.runner.shared.protocol.HeartbeatHandler.HeartbeatWebsocket;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.StatusCodeMappings;
import de.aaaaaaah.velcom.runner.shared.protocol.exceptions.SerializationException;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.ResetOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.UpdateBenchmarkRepoOrder;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The listener for thr websocket connection.
 */
public class WebsocketListener implements WebSocket.Listener, SocketConnectionManager,
	HeartbeatWebsocket {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketListener.class);

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
		flushTextBuffer();
		this.stateListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());
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
				flushTextBuffer();
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
					LOGGER.warn("Unknown message received", e);
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
				throw new UncheckedIOException(e);
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
			flushBinaryBuffer();
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
		LOGGER.info("Closing connection with status {} and reason '{}'", statusCode, reason);
		if (statusCode == StatusCodeMappings.NAME_ALREADY_TAKEN) {
			LOGGER.error(
				"Name already taken by a newly connected runner! "
					+ "This runner will exist now to prevent an infinite connecting"
					+ " ping-pong battle with the other runner."
			);
			System.exit(1);
		}
		disconnectImpl();
		return null;
	}

	@Override
	public void onError(WebSocket webSocket, Throwable error) {
		LOGGER.warn("Error in Websocket", error);
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
		LOGGER.info("Disconnecting...");
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
		} catch (ExecutionException e) {
			handleExecutionException(e);
		} catch (InterruptedException | TimeoutException e) {
			throw new ConnectionException(
				e.getMessage() == null ? "Error connecting" : e.getMessage(),
				e
			);
		}
	}

	private void handleExecutionException(ExecutionException e) {
		if (e.getCause() instanceof WebSocketHandshakeException) {
			throw new HandshakeFailureException(
				((WebSocketHandshakeException) e.getCause()).getResponse()
			);
		}
		throw new ConnectionException(
			e.getMessage() == null ? "Error connecting" : e.getMessage(),
			e
		);
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
		LOGGER.info("Server connection timed out.");
		disconnect();
	}

	@Override
	public boolean sendPing() {
		if (!isConnected()) {
			disconnectImpl();
			return false;
		}
		websocket.sendPing(ByteBuffer.allocate(Long.SIZE).putLong(System.currentTimeMillis()));
		return true;
	}

	private void disconnectImpl() {
		flushBinaryBuffer();
		flushTextBuffer();
		if (websocket != null) {
			websocket.sendClose(
				StatusCodeMappings.CLIENT_ORDERLY_DISCONNECT, "Client initiated close"
			)
				.thenAccept(WebSocket::abort)
				.whenComplete((success, error) -> {
					if (error != null) {
						LOGGER.info("Graceful disconnect not possible", error);
					}
					stateListeners.forEach(
						it -> it.onStateChange(ConnectionState.DISCONNECTED)
					);
				});
		} else {
			stateListeners.forEach(
				it -> it.onStateChange(ConnectionState.DISCONNECTED)
			);
		}
		if (heartbeatHandler != null) {
			heartbeatHandler.shutdown();
		}
		websocket = null;
	}

	private void flushTextBuffer() {
		textBuilder = new StringBuilder();
	}

	private void flushBinaryBuffer() {
		try {
			if (outputStream != null) {
				outputStream.close();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			outputStream = null;
		}
	}
}
