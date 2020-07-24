package de.aaaaaaah.velcom.backend;

import de.aaaaaaah.velcom.backend.data.benchrepo.BenchRepo;
import de.aaaaaaah.velcom.backend.runner_new.Dispatcher;
import de.aaaaaaah.velcom.backend.runner_new.ServerMasterWebsocketServlet;
import de.aaaaaaah.velcom.shared.protocol.serialization.Serializer;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.jetty.RoutingHandler;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.setup.Environment;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ServerFactory} that registers a listener on a different port to communicate with
 * runners.
 */
public class RunnerAwareServerFactory implements ServerFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunnerAwareServerFactory.class);

	private static final RunnerAwareServerFactory instance = new RunnerAwareServerFactory();

	private ServerFactory underlying;
	private GlobalConfig config;
	private Dispatcher dispatcher;
	private BenchRepo benchRepo;
	private final Serializer serializer;

	private RunnerAwareServerFactory() {
		this.serializer = new Serializer();
	}

	@Override
	public Server build(Environment environment) {
		ensureIsInitialized();

		Server server = underlying.build(environment);

		HttpConnectorFactory connectorFactory = new HttpConnectorFactory();
		connectorFactory.setPort(config.getRunnerPort());
		Connector connector = connectorFactory
			.build(server, environment.metrics(), "Runner", new ExecutorThreadPool(32));
		server.addConnector(
			connector
		);
		Map<Connector, Handler> handlerMap = new HashMap<>();

		for (Connector serverConnector : server.getConnectors()) {
			handlerMap.put(serverConnector, server.getHandler());
		}

		MutableServletContextHandler handler = new MutableServletContextHandler();
		handler.getServletContext().addServlet(
			"Runner Websocket servlet",
			new ServerMasterWebsocketServlet(dispatcher, serializer, config.getRunnerToken(), benchRepo)
		)
			.addMapping("/runner-connector");
		handlerMap.put(connector, handler);

		server.setHandler(new RoutingHandler(handlerMap));

		LOGGER.info("Registered the websocket servlet");

		return server;
	}

	@Override
	public void configure(Environment environment) {
		underlying.configure(environment);
	}


	/**
	 * Sets the config to use.
	 *
	 * @param config the config to use
	 */
	public void setConfig(GlobalConfig config) {
		if (this.config != null) {
			throw new IllegalStateException("I already have a config!");
		}
		this.config = config;
	}

	/**
	 * Sets the server factory to delegate to.
	 *
	 * @param underlying the underlying server factory
	 */
	public void setServerFactory(ServerFactory underlying) {
		if (this.underlying != null) {
			throw new IllegalStateException("I already have a server factory!");
		}

		this.underlying = underlying;
	}

	/**
	 * Sets the dispatcher to use.
	 *
	 * @param dispatcher the dispatcher
	 */
	public void setDispatcher(Dispatcher dispatcher) {
		if (this.dispatcher != null) {
			throw new IllegalStateException("I already have a dispatcher!");
		}
		this.dispatcher = dispatcher;
	}

	/**
	 * Sets the bench reo to use.
	 *
	 * @param benchRepo the bench repo
	 */
	public void setBenchRepo(BenchRepo benchRepo) {
		if (this.benchRepo != null) {
			throw new IllegalStateException("I already have a bench repo!");
		}
		this.benchRepo = benchRepo;
	}

	/**
	 * Returns whether the factory lacks an underlying server factory.
	 *
	 * @return true if no underlying server factory is available
	 */
	public boolean lacksFactory() {
		return underlying == null;
	}

	private void ensureIsInitialized() {
		if (config == null) {
			throw new IllegalStateException("Config not initialized");
		}
		if (underlying == null) {
			throw new IllegalStateException("Server factory not initialized");
		}
		if (dispatcher == null) {
			throw new IllegalStateException("Dispatcher not initialized");
		}
	}


	/**
	 * Returns the instance of the {@link RunnerAwareServerFactory}.
	 *
	 * @return the instance
	 */
	public static RunnerAwareServerFactory getInstance() {
		return instance;
	}
}
