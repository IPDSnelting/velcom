package de.aaaaaaah.velcom.runner.revision;

import de.aaaaaaah.velcom.runner.revision.states.Idle;
import de.aaaaaaah.velcom.runner.revision.states.RunnerState;
import de.aaaaaaah.velcom.runner.shared.Timeout;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Converter;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.serverbound.ServerBoundPacket;
import de.aaaaaaah.velcom.runner.shared.protocol.statemachine.StateMachine;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Connection implements WebSocket.Listener {

	private static final String AUTH_HEADER_NAME = "Authorization";
	private static final int CLOSE_CONNECTION_STATUS_CODE = 4000;
	private static final String CLOSE_CONNECTION_MESSAGE = "Client initiated close";
	private static final Duration CLOSE_CONNECTION_TIMEOUT = Duration.ofSeconds(10);

	private final StateMachine<RunnerState> stateMachine;
	private final Converter serializer;
	private StringBuilder textPacketBuilder;
	private final CompletableFuture<Void> closedFuture;
	private boolean closed;

	private final WebSocket socket;

	public Connection(Backend backend, URI address, String token)
		throws ExecutionException, InterruptedException {

		stateMachine = new StateMachine<>(new Idle(backend, this));
		serializer = new Converter();
		textPacketBuilder = new StringBuilder();
		closedFuture = new CompletableFuture<>();
		closed = false;

		socket = HttpClient.newHttpClient()
			.newWebSocketBuilder()
			.header(AUTH_HEADER_NAME, token)
			.buildAsync(address, null)
			.get();
	}

	public synchronized void sendPacket(ServerBoundPacket packet) {
		if (closed) {
			return;
		}

		serializer.serialize(packet).ifPresentOrElse(
			str -> socket.sendText(str, true),
			this::close
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

	public synchronized void close() {
		if (closed) {
			return;
		}

		// TODO move codes and messages to class that's shared between backend and runner
		socket.sendClose(CLOSE_CONNECTION_STATUS_CODE, CLOSE_CONNECTION_MESSAGE);

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
