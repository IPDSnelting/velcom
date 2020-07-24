package de.aaaaaaah.velcom.backend.runner_new;

import de.aaaaaaah.velcom.backend.data.benchrepo.BenchRepo;
import de.aaaaaaah.velcom.backend.runner_new.single.TeleRunner;
import de.aaaaaaah.velcom.shared.protocol.RunnerConnectionHeader;
import de.aaaaaaah.velcom.shared.protocol.RunnerDenyReason;
import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;
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
	private final Serializer serializer;
	private final String runnerToken;
	private final BenchRepo benchRepo;

	/**
	 * Creates a new runner servlet.
	 *
	 * @param dispatcher the dispatcher to connect to
	 * @param serializer the serializer to use for communication
	 * @param runnerToken the token runners need to provide when connecting
	 * @param benchRepo the benchmark repo
	 */
	public ServerMasterWebsocketServlet(Dispatcher dispatcher, Serializer serializer,
		String runnerToken, BenchRepo benchRepo) {
		this.dispatcher = dispatcher;
		this.serializer = serializer;
		this.runnerToken = runnerToken;
		this.benchRepo = benchRepo;
	}

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.getPolicy().setMaxTextMessageSize(Integer.MAX_VALUE);
		factory.setCreator((req, resp) -> {
			String name = req.getHeader(RunnerConnectionHeader.CONNECT_RUNNER_NAME.getName());
			String token = req.getHeader(RunnerConnectionHeader.CONNECT_RUNNER_TOKEN.getName());

			if (name == null || !runnerToken.equals(token)) {
				LOGGER.info("Runner from {} failed authentication!", req.getRemoteAddress());
				kickRunner(resp, RunnerDenyReason.TOKEN_INVALID);
				return null;
			}

			Optional<TeleRunner> existingRunner = dispatcher.getTeleRunner(name);

			TeleRunner myTeleRunner;
			if (existingRunner.isPresent()) {
				TeleRunner runner = existingRunner.get();
				if (runner.hasConnection()) {
					kickRunner(resp, RunnerDenyReason.NAME_ALREADY_USED);
					LOGGER.info(
						"Kicked runner {} from {} as the name as taken!", name, req.getRemoteAddress()
					);
					return null;
				}
				LOGGER.info("Reused runner connection for {} to ip {}!", name, req.getRemoteAddress());
				myTeleRunner = runner;
			} else {
				myTeleRunner = new TeleRunner(name, serializer, dispatcher, benchRepo);
				dispatcher.addRunner(myTeleRunner);
				LOGGER.info("Added runner {} from {}!", name, req.getRemoteAddress());
			}

			return myTeleRunner.createConnection();
		});
	}

	private void kickRunner(ServletUpgradeResponse response, RunnerDenyReason reason) {
		try {
			response.addHeader(
				RunnerConnectionHeader.DISCONNECT_DENY_REASON.getName(),
				reason.getHeaderValue()
			);
			response.sendError(reason.getCode(), reason.getMessage());
		} catch (IOException e) {
			LOGGER.warn("Failed to kick runner", e);
		}
	}
}