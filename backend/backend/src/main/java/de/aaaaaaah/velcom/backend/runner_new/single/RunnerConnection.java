package de.aaaaaaah.velcom.backend.runner_new.single;

import de.aaaaaaah.velcom.backend.runner_new.single.state.IdleState;
import de.aaaaaaah.velcom.backend.runner_new.single.state.TeleRunnerState;
import de.aaaaaaah.velcom.runner.shared.Timeout;
import de.aaaaaaah.velcom.runner.shared.protocol.HeartbeatHandler;
import de.aaaaaaah.velcom.runner.shared.protocol.HeartbeatHandler.HeartbeatWebsocket;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Converter;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound.ClientBoundPacket;
import de.aaaaaaah.velcom.runner.shared.protocol.statemachine.StateMachine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketFrameListener;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.api.extensions.Frame.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection with a single runner.
 */
public class RunnerConnection implements WebSocketListener, WebSocketFrameListener,
	HeartbeatWebsocket {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunnerConnection.class);

	@Nullable
	private Session session;
	@Nullable
	private ClosedBeforeConnect closedBeforeConnect;

	private final Converter serializer;
	private final StateMachine<TeleRunnerState> stateMachine;
	private final PeriodicStatusRequester periodicStatusRequester;
	private final List<Runnable> closeListeners;
	private final HeartbeatHandler heartbeatHandler;

	public RunnerConnection(Converter serializer, TeleRunner runner) {
		this.serializer = serializer;
		this.stateMachine = new StateMachine<>(new IdleState(runner, this));
		this.closeListeners = new ArrayList<>();

		this.periodicStatusRequester = new PeriodicStatusRequester(runner, this, stateMachine);
		this.heartbeatHandler = new HeartbeatHandler(this);

		addCloseListener(this.periodicStatusRequester::cancel);
		addCloseListener(this.heartbeatHandler::shutdown);
	}

	/**
	 * Adds a close listener.
	 *
	 * @param listener the close listener to add
	 */
	public void addCloseListener(Runnable listener) {
		synchronized (this.closeListeners) {
			this.closeListeners.add(listener);
		}
	}

	/**
	 * Returns the used serializer.
	 *
	 * @return the used serializer
	 */
	public Converter getSerializer() {
		return serializer;
	}

	/**
	 * Sends a packet to the runner.
	 * <br>
	 * If any error occurs, the connection is closed.
	 *
	 * @param packet the packet to send
	 * @throws IllegalArgumentException if the packet is not serializable
	 * @throws IllegalStateException if the connection is not yet connected
	 * @throws RuntimeIOException if an error occurred writing to the runner
	 */
	public void send(ClientBoundPacket packet) {
		Optional<String> serializedPacket = getSerializer().serialize(packet);

		if (serializedPacket.isEmpty()) {
			close(5000, "Internal server error");
			throw new IllegalArgumentException("Could not serialize packet " + packet);
		}
		if (session == null) {
			close(5000, "Internal server error");
			throw new IllegalStateException("Can not yet send packets");
		}
		try {
			session.getRemote().sendString(serializedPacket.get());
		} catch (IOException e) {
			close(5000, "Error sending packet");
			throw new RuntimeIOException(e);
		}
	}

	/**
	 * Closes the connecting, disconnecting the runner.
	 * <br>
	 * <p>If this is called before the session was opened, the session will be closed as soon as it is
	 * opened.</p>
	 *
	 * @param code the disconnect code
	 * @param reason the reason
	 */
	public synchronized void close(int code, String reason) {
		if (session == null) {
			closedBeforeConnect = new ClosedBeforeConnect(code, reason);
			return;
		}

		Timeout closeTimeout = Timeout.after(Duration.ofSeconds(10));
		closeTimeout.getCompletionStage().thenRun(this::hardDisconnect);
		closeTimeout.start();

		// Abort the timeout if we closed properly
		addCloseListener(closeTimeout::cancel);

		session.close(code, reason);
	}

	private synchronized void hardDisconnect() {
		try {
			if (session != null) {
				session.disconnect();
			}
		} catch (IOException e) {
			LOGGER.warn("Error in hard disconnect", e);
		}
	}

	@Override
	public void onWebSocketFrame(Frame frame) {
		if (frame.getType() == Type.PONG) {
			heartbeatHandler.onPong();
		}
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		// TODO: Disconnect codes
		close(5000, "Invalid binary payload received");
	}

	@Override
	public void onWebSocketText(String message) {
		stateMachine.changeCurrentState(state -> state.onText(message));
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		callCloseListeners();
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		callCloseListeners();
	}

	private void callCloseListeners() {
		synchronized (this.closeListeners) {
			this.closeListeners.forEach(Runnable::run);
		}
	}

	@Override
	public synchronized void onWebSocketConnect(Session session) {
		this.session = session;
		if (closedBeforeConnect != null) {
			close(closedBeforeConnect.code, closedBeforeConnect.reason);
		}

		this.periodicStatusRequester.start();
	}

	@Override
	public void onTimeoutDetected() {
		close(5000, "Ping timeout detected");
	}

	@Override
	public boolean sendPing() {
		if (session == null) {
			return false;
		}
		try {
			session.getRemote().sendPing(ByteBuffer.wrap(
				Long.toString(System.currentTimeMillis()).getBytes()
			));
		} catch (IOException e) {
			LOGGER.warn("Could not send a ping", e);
			return false;
		}
		return true;
	}

	private static class ClosedBeforeConnect {

		final int code;
		final String reason;

		ClosedBeforeConnect(int code, String reason) {
			this.code = code;
			this.reason = reason;
		}
	}
}
