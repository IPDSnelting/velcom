package de.aaaaaaah.designproto.runner.entity;

import de.aaaaaaah.designproto.runner.protocol.SocketConnectionManager;
import de.aaaaaaah.designproto.runner.shared.protocol.serialization.Serializer;
import de.aaaaaaah.designproto.runner.state.RunnerStateMachine;
import java.net.URI;

/**
 * The runner configuration. Mainly used as a cheap DI container in the stages.
 */
public class RunnerConfiguration {

	private final Serializer serializer;
	private final String runnerName;
	private final String runnerToken;
	private final URI serverUrl;
	private final SocketConnectionManager connectionManager;
	private final RunnerStateMachine runnerStateMachine;
	private final WorkExecutor workExecutor;
	private final BenchmarkRepoOrganizer benchmarkRepoOrganizer;

	/**
	 * Creates a new runner configuration.
	 *
	 * @param serializer the serializer to use
	 * @param runnerName the name of the runner
	 * @param runnerToken the runner authentication token
	 * @param connectionManager the connection manager to communicate with the server
	 * @param runnerStateMachine the runner state machine
	 * @param workExecutor the executor to use for received work
	 * @param serverUrl the url to use for connecting to the server
	 * @param benchmarkRepoOrganizer the benchmark repo organizer
	 */
	public RunnerConfiguration(Serializer serializer, String runnerName, String runnerToken,
		SocketConnectionManager connectionManager, RunnerStateMachine runnerStateMachine,
		WorkExecutor workExecutor, URI serverUrl,
		BenchmarkRepoOrganizer benchmarkRepoOrganizer) {
		this.serializer = serializer;
		this.runnerName = runnerName;
		this.runnerToken = runnerToken;
		this.connectionManager = connectionManager;
		this.runnerStateMachine = runnerStateMachine;
		this.workExecutor = workExecutor;
		this.serverUrl = serverUrl;
		this.benchmarkRepoOrganizer = benchmarkRepoOrganizer;
	}

	/**
	 * The name of this runner.
	 *
	 * @return the name of the runner
	 */
	public String getRunnerName() {
		return runnerName;
	}

	/**
	 * The used serializer.
	 *
	 * @return the used serializer
	 */
	public Serializer getSerializer() {
		return serializer;
	}

	/**
	 * Returns the connection manager to use.
	 *
	 * @return the socket connection manager
	 */
	public SocketConnectionManager getConnectionManager() {
		return connectionManager;
	}

	/**
	 * Returns the runner state machine.
	 *
	 * @return the state machine controlling the runner
	 */
	public RunnerStateMachine getRunnerStateMachine() {
		return runnerStateMachine;
	}

	/**
	 * Returns the work executor.
	 *
	 * @return the work executor
	 */
	public WorkExecutor getWorkExecutor() {
		return workExecutor;
	}

	/**
	 * Returns the benchmark repo organizer.
	 *
	 * @return the benchmark repo organizer
	 */
	public BenchmarkRepoOrganizer getBenchmarkRepoOrganizer() {
		return benchmarkRepoOrganizer;
	}

	/**
	 * Returns the uri pointing to the remote server.
	 *
	 * @return the uri pointing to the remote server
	 */
	public URI getServerUrl() {
		return serverUrl;
	}

	/**
	 * Returns the runner authentication token.
	 *
	 * @return the runner authentication token
	 */
	public String getRunnerToken() {
		return runnerToken;
	}
}
