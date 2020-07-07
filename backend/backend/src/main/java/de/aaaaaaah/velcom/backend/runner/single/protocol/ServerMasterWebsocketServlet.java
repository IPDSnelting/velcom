package de.aaaaaaah.velcom.backend.runner.single.protocol;

import de.aaaaaaah.velcom.backend.runner.Dispatcher;
import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.backend.runner.single.ServerRunnerStateMachine;
import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;
import java.io.IOException;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet that sets up the server side runner communication servlet.
 */
public class ServerMasterWebsocketServlet extends WebSocketServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(
		ServerMasterWebsocketServlet.class);

	private final Dispatcher dispatcher;
	private final Serializer serializer;
	private final String runnerToken;

	/**
	 * Creates a new runner servlet.
	 *
	 * @param dispatcher the dispatcher to connect to
	 * @param serializer the serializer to use for communication
	 * @param runnerToken the token runners need to provide when connecting
	 */
	public ServerMasterWebsocketServlet(Dispatcher dispatcher, Serializer serializer,
		String runnerToken) {
		this.dispatcher = dispatcher;
		this.serializer = serializer;
		this.runnerToken = runnerToken;
	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.getPolicy().setMaxTextMessageSize(Integer.MAX_VALUE);
		factory.setCreator((req, resp) -> {
			if (!runnerToken.equals(req.getHeader(HttpHeader.AUTHORIZATION.asString()))) {
				try {
					resp.sendForbidden(
						"Please provide a valid token in the "
							+ "'" + HttpHeader.AUTHORIZATION.asString() + "' header!"
					);
				} catch (IOException ignore) {
				}
				LOGGER.info("Runner from {} failed authentication!", req.getRemoteAddress());
				return null;
			}
			ServerRunnerStateMachine stateMachine = new ServerRunnerStateMachine(dispatcher);
			RunnerServerWebsocketListener listener = new RunnerServerWebsocketListener(
				serializer
			);
			ActiveRunnerInformation runnerInformation = new ActiveRunnerInformation(
				listener, stateMachine
			);

			listener.setRunnerInformation(runnerInformation);

			return listener;
		});
	}
}
