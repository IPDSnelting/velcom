package de.aaaaaaah.velcom.runner.revision;

import de.aaaaaaah.velcom.runner.revision.states.Idle;
import de.aaaaaaah.velcom.runner.revision.states.RunnerState;
import de.aaaaaaah.velcom.shared.Timeout;
import de.aaaaaaah.velcom.shared.protocol.RunnerConnectionHeader;
import de.aaaaaaah.velcom.shared.protocol.StatusCode;
import de.aaaaaaah.velcom.shared.protocol.serialization.Converter;
import de.aaaaaaah.velcom.shared.protocol.serialization.serverbound.ServerBoundPacket;
import de.aaaaaaah.velcom.shared.protocol.statemachine.StateMachine;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connection implements WebSocket.Listener {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunnerMain.class);

	private static final Duration CLOSE_CONNECTION_TIMEOUT = Duration.ofSeconds(10);

	private final StateMachine<RunnerState> stateMachine;
	private final Converter serializer;
	private StringBuilder textPacketBuilder;
	private final CompletableFuture<Void> closedFuture;
	private boolean closed;

	private final WebSocket socket;

	public Connection(TeleBackend teleBackend, URI address, String name, String token)
		throws ExecutionException, InterruptedException {

		stateMachine = new StateMachine<>(new Idle(teleBackend, this));
		serializer = new Converter();
		textPacketBuilder = new StringBuilder();
		closedFuture = new CompletableFuture<>();
		closed = false;

		LOGGER.debug("Opening connection to " + address);
		socket = HttpClient.newHttpClient()
			.newWebSocketBuilder()
			.header(RunnerConnectionHeader.CONNECT_RUNNER_NAME.getName(), name)
			.header(RunnerConnectionHeader.CONNECT_RUNNER_TOKEN.getName(), token)
			.buildAsync(address, this)
			.get();
		LOGGER.debug("Successfully opened connection to " + address);
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

	private synchronized void cleanupAfterClosed() {
		if (closed) {
			return;
		}
		closed = true;

		stateMachine.stop();
		closedFuture.complete(null);
	}

	public synchronized void close(StatusCode statusCode) {
		if (closed) {
			return;
		}

		socket.sendClose(statusCode.getCode(), statusCode.getDescriptionAsReason());

		Timeout disconnectTimeout = Timeout.after(CLOSE_CONNECTION_TIMEOUT);
		disconnectTimeout.getCompletionStage().thenAccept(aVoid -> socket.abort());
		disconnectTimeout.start();
	}

	// TODO maybe not expose the state machine?
	public StateMachine<RunnerState> getStateMachine() {
		return stateMachine;
	}

	public Converter getSerializer() {
		return serializer;
	}

	public Future<Void> getClosedFuture() {
		return closedFuture;
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

		cleanupAfterClosed();
		return null;
	}

	@Override
	public synchronized void onError(WebSocket webSocket, Throwable error) {
		cleanupAfterClosed();
	}
}
