package de.aaaaaaah.velcom.backend.runner.single.protocol;

import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.backend.runner.single.RunnerConnectionManager;
import de.aaaaaaah.velcom.runner.shared.protocol.HeartbeatHandler;
import de.aaaaaaah.velcom.runner.shared.protocol.HeartbeatHandler.HeartbeatWebsocket;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.StatusCodeMappings;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.Serializer;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.WorkReceived;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketFrameListener;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.api.extensions.Frame.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to websocket messages from the runner and keeps the connection alive.
 */
public class RunnerServerWebsocketListener implements WebSocketListener, WebSocketFrameListener,
	HeartbeatWebsocket, RunnerConnectionManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(
		RunnerServerWebsocketListener.class);

	private HeartbeatHandler heartbeatHandler;
	private Session session;
	private ActiveRunnerInformation runnerInformation;
	private Serializer serializer;

	/**
	 * Creates a new websocket listener.
	 *
	 * @param serializer the serializer to use
	 */
	public RunnerServerWebsocketListener(Serializer serializer) {
		this.serializer = serializer;
		this.heartbeatHandler = new HeartbeatHandler(this);
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
		LOGGER.warn(
			"Runner sent us a binary transmission, kicking it! [{}]",
			runnerInformation.getRunnerInformation()
		);
		disconnect();
	}

	@Override
	public void onWebSocketText(String message) {
		runnerInformation.setLastReceivedMessage(Instant.now());
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
				LOGGER.warn(
					"Runner send an invalid packet type ({}) from {}",
					type, runnerInformation.getRunnerInformation()
				);
				return;
		}
		runnerInformation.getRunnerStateMachine().onMessageReceived(type, entity);
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		LOGGER.info(
			"Closed connection with {} - {} for {}",
			statusCode, reason, runnerInformation.getRunnerInformation()
		);
		heartbeatHandler.shutdown();
		runnerInformation.setDisconnected(statusCode);
	}

	@Override
	public void onWebSocketConnect(Session session) {
		this.session = session;
		this.runnerInformation.getRunnerStateMachine().onConnectionOpened(runnerInformation);
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		cause.printStackTrace();
		disconnectImpl();
	}

	@Override
	public void onTimeoutDetected() {
		LOGGER.info("Runner timed out [{}]", runnerInformation.getRunnerInformation());
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
			runnerInformation.setLastReceivedMessage(Instant.now());
		}
	}

	@Override
	public void disconnect() {
		disconnectImpl();
	}

	@Override
	public void disconnect(int status, String message) {
		disconnectImpl(status, message);
	}

	@Override
	public void sendEntity(SentEntity entity) throws IOException {
		ProtocolHelper.sendObject(session, entity, serializer);
	}

	@Override
	public OutputStream createBinaryOutputStream() {
		return ProtocolHelper.createBinaryOutputStream(session);
	}

	private void disconnectImpl() {
		disconnectImpl(
			StatusCodeMappings.SERVER_INITIATED_DISCONNECT, "Server initiated close"
		);
	}

	private void disconnectImpl(int statusCode, String reason) {
		if (session != null && session.isOpen()) {
			// calls onClose which can call the state listeners
			session.close(statusCode, reason);
		} else {
			runnerInformation.setDisconnected(statusCode);
		}
		if (heartbeatHandler != null) {
			heartbeatHandler.shutdown();
		}
		this.session = null;
	}
}
