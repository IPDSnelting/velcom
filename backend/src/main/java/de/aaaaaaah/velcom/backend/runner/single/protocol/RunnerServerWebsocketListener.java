package de.aaaaaaah.velcom.backend.runner.single.protocol;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.backend.runner.single.RunnerConnectionManager;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.HeartbeatHandler;
import de.aaaaaaah.velcom.runner.shared.protocol.HeartbeatHandler.HeartbeatWebsocket;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Serializer;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.WorkReceived;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketFrameListener;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.api.extensions.Frame.Type;

/**
 * Listens to websocket messages from the runner and keeps the connection alive.
 */
public class RunnerServerWebsocketListener implements WebSocketListener, WebSocketFrameListener,
	HeartbeatWebsocket, RunnerConnectionManager {

	private HeartbeatHandler heartbeatHandler;
	private Session session;
	private ActiveRunnerInformation runnerInformation;
	private Serializer serializer;
	private List<ConnectionStateListener> connectionStateListener;

	/**
	 * Creates a new websocket listener.
	 *
	 * @param serializer the serializer to use
	 */
	public RunnerServerWebsocketListener(Serializer serializer) {
		this.serializer = serializer;
		this.heartbeatHandler = new HeartbeatHandler(this);
		this.connectionStateListener = new ArrayList<>();
	}

	/**
	 * Sets the runner information.
	 *
	 * @param runnerInformation the runner information
	 */
	public void setRunnerInformation(ActiveRunnerInformation runnerInformation) {
		this.runnerInformation = runnerInformation;
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		System.err.println("Received a binary transmission");
	}

	@Override
	public void onWebSocketText(String message) {
		SentEntity entity;
		String type = serializer.peekType(message);
		switch (type) {
			case "WorkReceived":
				entity = serializer.deserialize(message, WorkReceived.class);
				break;
			case "BenchmarkResults":
				entity = serializer.deserialize(message, BenchmarkResults.class);
				break;
			case "RunnerInformation":
				entity = serializer.deserialize(message, RunnerInformation.class);
				break;
			default:
				System.err.println("Unknwon type received: " + message);
				return;
		}
		runnerInformation.getRunnerStateMachine().onMessageReceived(type, entity);
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		System.out.println("Closing: statusCode = " + statusCode + ", reason = " + reason);
		heartbeatHandler.shutdown();
		connectionStateListener.forEach(it -> it.onStateChanged(ConnectionState.DISCONNECTED));
		runnerInformation.setState(RunnerStatusEnum.DISCONNECTED);
	}

	@Override
	public void onWebSocketConnect(Session session) {
		this.session = session;
		this.runnerInformation.getRunnerStateMachine().onConnectionOpened(runnerInformation);
		this.connectionStateListener.forEach(it -> it.onStateChanged(ConnectionState.CONNECTED));
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		cause.printStackTrace();
		disconnectImpl();
	}

	@Override
	public void onTimeoutDetected() {
		System.out.println("Timeout detected :(");
		disconnect();
	}

	@Override
	public boolean sendPing() {
		try {
			session.getRemote().sendPing(
				ByteBuffer.allocate(Long.SIZE).putLong(System.currentTimeMillis())
			);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void onWebSocketFrame(Frame frame) {
		if (frame.getType() == Type.PONG) {
			heartbeatHandler.onPong();
		}
	}

	@Override
	public void disconnect() {
		disconnectImpl();
	}

	@Override
	public void sendEntity(SentEntity entity) throws IOException {
		ProtocolHelper.sendObject(session, entity, serializer);
	}

	@Override
	public OutputStream createBinaryOutputStream() {
		return ProtocolHelper.createBinaryOutputStream(session);
	}

	@Override
	public void addConnectionStateListener(ConnectionStateListener listener) {
		connectionStateListener.add(listener);
	}

	@Override
	public void removeConnectionStateListener(ConnectionStateListener listener) {
		connectionStateListener.remove(listener);
	}

	private void disconnectImpl() {
		if (session != null && session.isOpen()) {
			// calls onClose which can call the state listeners
			session.close(4002, "Server initiated close");
		} else {
			connectionStateListener.forEach(it -> it.onStateChanged(ConnectionState.DISCONNECTED));
			runnerInformation.setState(RunnerStatusEnum.DISCONNECTED);
		}
		if (heartbeatHandler != null) {
			heartbeatHandler.shutdown();
		}
		this.session = null;
	}
}
