package de.aaaaaaah.velcom.backend.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.data.benchrepo.BenchRepo;
import de.aaaaaaah.velcom.backend.runner.single.RunnerConnection;
import de.aaaaaaah.velcom.backend.runner.single.TeleRunner;
import de.aaaaaaah.velcom.shared.protocol.RunnerConnectionHeader;
import de.aaaaaaah.velcom.shared.protocol.RunnerDenyReason;
import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerMasterWebsocketServletTest {

	private ServerMasterWebsocketServlet servlet;
	private String runnerName;

	private WebSocketCreator creator;
	private String runnerToken;
	private ServletUpgradeRequest upgradeRequest;
	private ServletUpgradeResponse upgradeResponse;
	private Dispatcher dispatcher;

	@BeforeEach
	void setUp() {
		runnerToken = "runner token";
		runnerName = "name";

		this.upgradeRequest = mock(ServletUpgradeRequest.class);
		when(upgradeRequest.getHeader(RunnerConnectionHeader.CONNECT_RUNNER_NAME.getName()))
			.thenReturn(runnerName);
		when(upgradeRequest.getHeader(RunnerConnectionHeader.CONNECT_RUNNER_TOKEN.getName()))
			.thenReturn(runnerToken);

		upgradeResponse = mock(ServletUpgradeResponse.class);
		dispatcher = mock(Dispatcher.class);

		servlet = new ServerMasterWebsocketServlet(
			dispatcher,
			mock(Serializer.class),
			runnerToken,
			mock(BenchRepo.class)
		);
		WebSocketServerFactory factory = new WebSocketServerFactory();
		servlet.configure(factory);
		this.creator = factory.getCreator();
	}

	@Test
	void rejectsConnectionWithoutAuthHeader() throws Exception {
		when(upgradeRequest.getHeader(RunnerConnectionHeader.CONNECT_RUNNER_TOKEN.getName()))
			.thenReturn(null);

		assertRejectedInvalidToken();
	}

	@Test
	void rejectsConnectionWithoutName() throws Exception {
		when(upgradeRequest.getHeader(RunnerConnectionHeader.CONNECT_RUNNER_NAME.getName()))
			.thenReturn(null);

		assertRejectedInvalidToken();
	}

	@Test
	void rejectsConnectionWithInvalidToken() throws Exception {
		when(upgradeRequest.getHeader(RunnerConnectionHeader.CONNECT_RUNNER_TOKEN.getName()))
			.thenReturn("hello");

		assertRejectedInvalidToken();
	}

	@Test
	void rejectsNameAlreadyTaken() throws Exception {
		TeleRunner runner = mock(TeleRunner.class);
		when(runner.hasConnection()).thenReturn(true);
		when(dispatcher.getTeleRunner(runnerName)).thenReturn(Optional.of(runner));

		assertThat(requestConnection()).isNull();
		verify(upgradeResponse).sendError(
			RunnerDenyReason.NAME_ALREADY_USED.getCode(),
			RunnerDenyReason.NAME_ALREADY_USED.getMessage()
		);
		verify(upgradeResponse).addHeader(
			RunnerConnectionHeader.DISCONNECT_DENY_REASON.getName(),
			RunnerDenyReason.NAME_ALREADY_USED.getHeaderValue()
		);
		verify(dispatcher, never()).addRunner(any());
	}

	@Test
	void allowsSameNameWhenDisconnected() {
		TeleRunner runner = mock(TeleRunner.class);
		RunnerConnection connection = mock(RunnerConnection.class);

		when(runner.hasConnection()).thenReturn(false);
		when(runner.createConnection()).thenReturn(connection);
		when(runner.isDisposed()).thenReturn(false);
		when(dispatcher.getTeleRunner(runnerName)).thenReturn(Optional.of(runner));

		assertThat(requestConnection()).isSameAs(connection);
		// Does not yet add it, as it is a new runner and needs to be ready first
		verify(dispatcher, never()).addRunner(runner);
	}

	@Test
	void allowsSameNameWhenDisconnectedAndDisposed() {
		TeleRunner runner = mock(TeleRunner.class);
		RunnerConnection connection = mock(RunnerConnection.class);
		when(runner.hasConnection()).thenReturn(false);
		when(runner.createConnection()).thenReturn(connection);
		when(runner.isDisposed()).thenReturn(true);
		when(dispatcher.getTeleRunner(runnerName)).thenReturn(Optional.of(runner));

		assertThat(requestConnection()).isNotNull().isNotSameAs(connection);
		// Does not yet add it, as it is a new runner and needs to be ready first
		verify(dispatcher, never()).addRunner(runner);
	}

	@Test
	void acceptsNewRunner() {
		when(dispatcher.getTeleRunner(runnerName)).thenReturn(Optional.empty());

		assertThat(requestConnection()).isNotNull();
		// Does not yet add it, as it is a new runner and needs to be ready first
		verify(dispatcher, never()).addRunner(any());
	}

	private void assertRejectedInvalidToken() throws IOException {
		assertThat(requestConnection()).isNull();
		verify(upgradeResponse).sendError(
			RunnerDenyReason.TOKEN_INVALID.getCode(),
			RunnerDenyReason.TOKEN_INVALID.getMessage()
		);
		verify(upgradeResponse).addHeader(
			RunnerConnectionHeader.DISCONNECT_DENY_REASON.getName(),
			RunnerDenyReason.TOKEN_INVALID.getHeaderValue()
		);
		verify(dispatcher, never()).addRunner(any());
	}

	private Object requestConnection() {
		return creator.createWebSocket(upgradeRequest, upgradeResponse);
	}

}
