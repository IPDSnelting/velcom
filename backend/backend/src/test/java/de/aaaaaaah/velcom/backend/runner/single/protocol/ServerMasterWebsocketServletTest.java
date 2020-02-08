package de.aaaaaaah.velcom.backend.runner.single.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.aaaaaaah.velcom.backend.runner.Dispatcher;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.SimpleJsonSerializer;
import java.io.IOException;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ServerMasterWebsocketServletTest {

	private static final String TOKEN = "Test token";

	private ServerMasterWebsocketServlet servlet;
	private Dispatcher dispatcher;
	private SimpleJsonSerializer serializer;

	@BeforeEach
	void setUp() {
		dispatcher = mock(Dispatcher.class);

		serializer = new SimpleJsonSerializer();
		servlet = new ServerMasterWebsocketServlet(dispatcher, serializer, TOKEN);
	}

	@Test
	void disconnectsRunnersWithoutAuthentication() throws IOException {
		WebSocketServletFactory factory = mock(WebSocketServletFactory.class);
		when(factory.getPolicy()).thenReturn(mock(WebSocketPolicy.class));
		servlet.configure(factory);

		ArgumentCaptor<WebSocketCreator> captor = ArgumentCaptor.forClass(WebSocketCreator.class);

		verify(factory).setCreator(captor.capture());

		WebSocketCreator creator = captor.getValue();

		ServletUpgradeRequest request = mock(ServletUpgradeRequest.class);
		ServletUpgradeResponse response = mock(ServletUpgradeResponse.class);

		assertThat(creator.createWebSocket(request, response)).isNull();

		verify(response).sendForbidden(anyString());
	}

	@Test
	void disconnectsRunnersWithWrongAuthentication() throws IOException {
		WebSocketServletFactory factory = mock(WebSocketServletFactory.class);
		when(factory.getPolicy()).thenReturn(mock(WebSocketPolicy.class));
		servlet.configure(factory);

		ArgumentCaptor<WebSocketCreator> captor = ArgumentCaptor.forClass(WebSocketCreator.class);

		verify(factory).setCreator(captor.capture());

		WebSocketCreator creator = captor.getValue();

		ServletUpgradeRequest request = mock(ServletUpgradeRequest.class);
		ServletUpgradeResponse response = mock(ServletUpgradeResponse.class);

		when(request.getHeader("Authorization")).thenReturn("WRONG");

		assertThat(creator.createWebSocket(request, response)).isNull();
		verify(response).sendForbidden(anyString());
	}

	@Test
	void acceptsRunnersWithCorrectAuthentication() {
		WebSocketServletFactory factory = mock(WebSocketServletFactory.class);
		when(factory.getPolicy()).thenReturn(mock(WebSocketPolicy.class));
		servlet.configure(factory);

		ArgumentCaptor<WebSocketCreator> captor = ArgumentCaptor.forClass(WebSocketCreator.class);

		verify(factory).setCreator(captor.capture());

		WebSocketCreator creator = captor.getValue();

		ServletUpgradeRequest request = mock(ServletUpgradeRequest.class);
		ServletUpgradeResponse response = mock(ServletUpgradeResponse.class);

		when(request.getHeader("Authorization")).thenReturn(TOKEN);

		Object servelet = creator.createWebSocket(request, response);

		assertThat(servelet).isInstanceOf(RunnerServerWebsocketListener.class);

		((RunnerServerWebsocketListener) servelet).disconnect();
	}

}