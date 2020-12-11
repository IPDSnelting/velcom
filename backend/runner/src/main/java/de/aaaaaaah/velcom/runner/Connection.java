package de.aaaaaaah.velcom.runner;

import de.aaaaaaah.velcom.runner.states.Idle;
import de.aaaaaaah.velcom.runner.states.RunnerState;
import de.aaaaaaah.velcom.shared.protocol.HeartbeatHandler;
import de.aaaaaaah.velcom.shared.protocol.HeartbeatHandler.HeartbeatWebsocket;
import de.aaaaaaah.velcom.shared.protocol.RunnerConnectionHeader;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.ServerBoundPacket;
import de.aaaaaaah.velcom.shared.protocol.statemachine.StateMachine;
import de.aaaaaaah.velcom.shared.util.Timeout;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connection implements WebSocket.Listener, HeartbeatWebsocket {

	private static final Logger LOGGER = LoggerFactory.getLogger(Connection.class);

	private final StateMachine<RunnerState> stateMachine;
	private final Serializer serializer;
	private StringBuilder textPacketBuilder;
	private final CompletableFuture<Void> closedFuture;
	private boolean closed;

	// Initialized in #onOpen(). Should be initialized once constructor completes.
	private WebSocket socket;
	private HeartbeatHandler heartbeatHandler;

	public Connection(TeleBackend teleBackend, URI address, String name, String token)
		throws ExecutionException, InterruptedException {

		stateMachine = new StateMachine<>(new Idle(teleBackend, this));
		serializer = new Serializer();
		textPacketBuilder = new StringBuilder();
		closedFuture = new CompletableFuture<>();
		closed = false;

		LOGGER.debug("Opening connection to {}", address);
		HttpClient.newHttpClient()
			.newWebSocketBuilder()
			.header(RunnerConnectionHeader.CONNECT_RUNNER_NAME.getName(), name)
			.header(RunnerConnectionHeader.CONNECT_RUNNER_TOKEN.getName(), token)
			.buildAsync(address, this)
			.get();
		// At this point, #onOpen() should have been called already, so the socket and heartbeatHandler
		// have been initialized.
		LOGGER.debug("Successfully opened connection to {}", address);
	}

	public synchronized void sendPacket(ServerBoundPacket packet) {
		if (closed) {
			return;
		}

		serializer.serialize(packet).ifPresentOrElse(
			str -> socket.sendText(str, true),
			() -> close(StatusCode.ILLEGAL_PACKET)
		);
	}

	/**
	 * Do whatever needs to do when the connection transitions into the closed state. This function is
	 * threadsafe and can be called multiple times.
	 */
	private synchronized void cleanupAfterClosed() {
		if (closed) {
			return;
		}
		closed = true;

		LOGGER.debug("Cleaning up after closing the connection");
		stateMachine.stop();
		closedFuture.complete(null);
		heartbeatHandler.shutdown();
	}

	public synchronized void close(StatusCode statusCode) {
		if (closed) {
			return;
		}

		LOGGER.warn("Closing connection: {}", statusCode.getDescription());
		socket.sendClose(statusCode.getCode(), statusCode.getDescriptionAsReason());

		Timeout disconnectTimeout = Timeout.after(Delays.CLOSE_CONNECTION_TIMEOUT);
		disconnectTimeout.getCompletionStage().thenAccept(aVoid -> forceClose(statusCode));
		disconnectTimeout.start();
	}

	public synchronized void forceClose(StatusCode statusCode) {
		if (closed) {
			return;
		}

		LOGGER.warn("Force-closing connection: {}", statusCode.getDescription());
		socket.abort();

		// Since neither onClose nor onError are called when the socket is aborted like this, we
		// need to call cleanupAfterClosed here too.
		cleanupAfterClosed();
	}

	public boolean switchFromRestingState(RunnerState state) throws InterruptedException {
		return stateMachine.switchFromRestingState(state);
	}

	public Serializer getSerializer() {
		return serializer;
	}

	public Future<Void> getClosedFuture() {
		return closedFuture;
	}

	@Override
	public void onOpen(WebSocket webSocket) {
		webSocket.request(1);

		// Initial call by the websocket library, so we can use it to obtain our socket for socket-y
		// purposes.
		synchronized (this) {
			socket = webSocket;
			heartbeatHandler = new HeartbeatHandler(this);
		}
	}

	@Override
	public synchronized CompletionStage<?> onText(WebSocket webSocket, CharSequence data,
		boolean last) {

		webSocket.request(1);
		if (closed) {
			return null;
		}

		textPacketBuilder.append(data);

		if (last) {
			String text = textPacketBuilder.toString();
			textPacketBuilder = new StringBuilder();
			stateMachine.changeCurrentState(state -> state.onText(text));
		}

		return null;
	}

	@Override
	public synchronized CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data,
		boolean last) {

		webSocket.request(1);
		if (closed) {
			return null;
		}

		stateMachine.changeCurrentState(state -> state.onBinary(data, last));

		return null;
	}

	@Override
	public synchronized CompletionStage<?> onClose(WebSocket webSocket, int statusCode,
		String reason) {

		String statusCodeStr = StatusCode.fromCode(statusCode)
			.map(Enum::toString)
			.orElse(Integer.toString(statusCode));
		LOGGER.debug("Connection closed normally, status code: {}", statusCodeStr);
		cleanupAfterClosed();
		return null;
	}

	@Override
	public synchronized void onError(WebSocket webSocket, Throwable error) {
		// For some reason, this function is not called after a socket.abort().
		LOGGER.debug("Connection closed abnormally");
		cleanupAfterClosed();
	}

	@Override
	public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
		webSocket.request(1);
		heartbeatHandler.onPong();
		return null;
	}

	@Override
	public void onTimeoutDetected() {
		LOGGER.warn("Detected ping timeout, disconnecting");
		close(StatusCode.PING_TIMEOUT);
	}

	@Override
	public boolean sendPing() {
		synchronized (this) {
			if (closed) {
				LOGGER.debug("Couldn't send ping because connection already closed");
				return false;
			}

			socket.sendPing(ByteBuffer.wrap(Long.toString(System.currentTimeMillis()).getBytes()));
			return true;
		}
	}
}
