package de.aaaaaaah.velcom.backend.runner_new.single;

import de.aaaaaaah.velcom.backend.runner_new.single.state.IdleState;
import de.aaaaaaah.velcom.backend.runner_new.single.state.TeleRunnerState;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Converter;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.clientbound.ClientBoundPacket;
import de.aaaaaaah.velcom.runner.shared.protocol.statemachine.StateMachine;
import java.io.IOException;
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

/**
 * A connection with a single runner.
 */
public class RunnerConnection implements WebSocketListener, WebSocketFrameListener {

	@Nullable
	private Session session;
	@Nullable
	private ClosedBeforeConnect closedBeforeConnect;

	private final Converter serializer;
	private final StateMachine<TeleRunnerState> stateMachine;
	private final PeriodicStatusRequester periodicStatusRequester;
	private final List<Runnable> closeListeners;

	public RunnerConnection(Converter serializer, TeleRunner runner) {
		this.serializer = serializer;
		this.stateMachine = new StateMachine<>(new IdleState(runner, this));
		this.closeListeners = new ArrayList<>();

		this.periodicStatusRequester = new PeriodicStatusRequester(runner, this, stateMachine);

		addCloseListener(this.periodicStatusRequester::cancel);
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
			disconnect(5000, "Internal server error");
			throw new IllegalArgumentException("Could not serialize packet " + packet);
		}
		if (session == null) {
			disconnect(5000, "Internal server error");
			throw new IllegalStateException("Can not yet send packets");
		}
		try {
			session.getRemote().sendString(serializedPacket.get());
		} catch (IOException e) {
			disconnect(5000, "Error sending packet");
			throw new RuntimeIOException(e);
		}
	}

	/**
	 * Disconnects the runner.
	 *
	 * @param code the disconnect code
	 * @param reason the reason
	 */
	public synchronized void disconnect(int code, String reason) {
		// TODO: Hard disconnect after timeout
		if (session != null) {
			session.close(code, reason);
		} else {
			closedBeforeConnect = new ClosedBeforeConnect(code, reason);
		}
	}

	@Override
	public void onWebSocketFrame(Frame frame) {
		if (frame.getType() == Type.PONG) {
			// TODO: Implement ping pong
			return;
		}
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		// TODO: Disconnect codes
		disconnect(5000, "Invalid binary payload received");
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
			disconnect(closedBeforeConnect.code, closedBeforeConnect.reason);
		}

		this.periodicStatusRequester.start();
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
