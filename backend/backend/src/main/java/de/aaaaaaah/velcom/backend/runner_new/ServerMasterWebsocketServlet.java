package de.aaaaaaah.velcom.backend.runner_new;

import de.aaaaaaah.velcom.backend.runner_new.single.TeleRunner;
import de.aaaaaaah.velcom.shared.protocol.serialization.Converter;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet that sets up the server side runner communication servlet.
 */
public class ServerMasterWebsocketServlet extends WebSocketServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerMasterWebsocketServlet.class);

	private final Dispatcher dispatcher;
	private final Converter serializer;
	private final String runnerToken;

	/**
	 * Creates a new runner servlet.
	 *
	 * @param dispatcher the dispatcher to connect to
	 * @param serializer the serializer to use for communication
	 * @param runnerToken the token runners need to provide when connecting
	 */
	public ServerMasterWebsocketServlet(Dispatcher dispatcher, Converter serializer,
		String runnerToken) {
		this.dispatcher = dispatcher;
		this.serializer = serializer;
		this.runnerToken = runnerToken;
	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.getPolicy().setMaxTextMessageSize(Integer.MAX_VALUE);
		factory.setCreator((req, resp) -> {
			String name = req.getHeader("Runner-Name");
			String token = req.getHeader("Runner-Token");

			if (name == null || !runnerToken.equals(token)) {
				LOGGER.info("Runner from {} failed authentication!", req.getRemoteAddress());
				kickRunnerToken(resp);
				return null;
			}

			Optional<TeleRunner> existingRunner = dispatcher.getTeleRunner(name);

			TeleRunner myTeleRunner;
			if (existingRunner.isPresent()) {
				TeleRunner runner = existingRunner.get();
				if (runner.hasConnection()) {
					kickRunnerName(resp);
					return null;
				}
				myTeleRunner = runner;
			} else {
				myTeleRunner = new TeleRunner(name, serializer, dispatcher);
			}

			return myTeleRunner.createConnection();
		});
	}

	private void kickRunnerToken(ServletUpgradeResponse response) {
		// TODO: 07.07.20 Refactor headers into a Enum 
		response.addHeader("Runner-Deny", "TOKEN");
		kickRunner(response, 401, "Your token is invalid.");
	}

	private void kickRunnerName(ServletUpgradeResponse response) {
		response.addHeader("Runner-Deny", "NAME");
		kickRunner(response, 403, "Your name is already taken");
	}

	private void kickRunner(ServletUpgradeResponse response, int code, String description) {
		try {
			response.sendError(code, description);
		} catch (IOException e) {
			LOGGER.info("Failed to kick runner", e);
		}
	}
}
