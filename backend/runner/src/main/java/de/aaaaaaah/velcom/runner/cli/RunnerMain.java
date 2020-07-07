package de.aaaaaaah.velcom.runner.cli;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import de.aaaaaaah.velcom.runner.cli.RunnerCliSpec_Parser.HelpRequested;
import de.aaaaaaah.velcom.runner.cli.RunnerCliSpec_Parser.ParseResult;
import de.aaaaaaah.velcom.runner.cli.RunnerCliSpec_Parser.ParsingFailed;
import de.aaaaaaah.velcom.runner.cli.RunnerCliSpec_Parser.ParsingSuccess;
import de.aaaaaaah.velcom.runner.entity.BenchmarkRepoOrganizer;
import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.entity.TempFileBenchmarkRepoOrganizer;
import de.aaaaaaah.velcom.runner.entity.execution.BenchmarkscriptWorkExecutor;
import de.aaaaaaah.velcom.runner.exceptions.ConnectionException;
import de.aaaaaaah.velcom.runner.exceptions.HandshakeFailureException;
import de.aaaaaaah.velcom.runner.protocol.ReestablishConnectionListener;
import de.aaaaaaah.velcom.runner.protocol.WebsocketListener;
import de.aaaaaaah.velcom.shared.protocol.serialization.SimpleJsonSerializer;
import de.aaaaaaah.velcom.runner.state.RunnerStateMachine;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/**
 * The runner main class.
 */
public class RunnerMain {

	/**
	 * Shuts down the JVM. Can be mocked if you want to replace it for tests.
	 */
	interface SystemExiter {

		/**
		 * Shut down the system.
		 *
		 * @param code the exit code
		 */
		void exit(int code);
	}

	/**
	 * The exiter that the main method uses.
	 */
	static SystemExiter exiter = System::exit;

	/**
	 * Called by the vm, the main entry point.
	 *
	 * @param args the command line arguments
	 * @throws InterruptedException if the main sleeper thread is interrupted
	 */
	public static void main(String[] args) throws InterruptedException {
		ParseResult parseResult = new RunnerCliSpec_Parser().parse(args);

		if (parseResult instanceof HelpRequested) {
			new RunnerCliSpec_Parser().printOnlineHelp(System.err);
			exiter.exit(0);
			return;
		} else if (parseResult instanceof ParsingFailed) {
			System.err.println("Error: " + ((ParsingFailed) parseResult).getError().getMessage());
			System.err.println();
			System.err.println();
			new RunnerCliSpec_Parser().printOnlineHelp(System.err);
			exiter.exit(0);
			return;
		}
		RunnerCliSpec cliSpec = ((ParsingSuccess) parseResult).getResult();

		RunnerConfigPojo configPojo = getConfig(cliSpec);

		WebsocketListener websocketListener = new WebsocketListener();

		RunnerConfiguration runnerConfiguration = new RunnerConfiguration(
			new SimpleJsonSerializer(),
			configPojo.getRunnerName(),
			configPojo.getRunnerToken(),
			websocketListener,
			new RunnerStateMachine(),
			new BenchmarkscriptWorkExecutor(),
			configPojo.getServerUrl(),
			createBenchmarkRepoOrganizer()
		);
		websocketListener.setConfiguration(runnerConfiguration);

		System.err.println();
		System.err.println("---- Runner Details ----");
		System.err.println("Name       : " + runnerConfiguration.getRunnerName());
		System.err.println("Server URL : " + runnerConfiguration.getServerUrl());
		System.err.println("------------------------");
		System.err.println();

		System.err.println(
			"I will keep trying to connect and reconnect until you kill me (SIGTERM)\n"
		);

		ReestablishConnectionListener reconnectListener = new ReestablishConnectionListener(
			websocketListener
		);
		websocketListener.addStateListener(reconnectListener);

		// Initial connect
		try {
			websocketListener.connect();
		} catch (HandshakeFailureException e) {
			System.err.println("\n========= HANDSHAKE FAILED =========");

			System.err.println(
				"Handshake with server failed with response '" + e.getResponse() + "'"
			);
			if (e.isAuthenticationFailure()) {
				System.err.println("\n====== RESPONSE INTERPRETATION ======");
				System.err.println("Invalid credentials, please check them!");
				System.err.println("I will exit now, as this error is likely not recoverable!");
				exiter.exit(1);
				return;
			}
		} catch (ConnectionException e) {
			System.err.println("Initial server connection failed with '" + e.getMessage() + "'\n");
		}

		// Start reconnection attempts
		reconnectListener.scheduleReconnect();

		// Graceful shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(websocketListener::disconnect));

		// That was my goal.
		//noinspection InfiniteLoopStatement
		while (true) {
			Thread.sleep(10_000);
		}
	}

	private static RunnerConfigPojo getConfig(RunnerCliSpec cliSpec) {
		ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new ParameterNamesModule());
		try {
			return objectMapper.readValue(
				cliSpec.configFileLocation().toFile(), RunnerConfigPojo.class
			);
		} catch (IOException e) {
			die(e, "Error reading config!");
			// never reached
			return null;
		}
	}

	private static BenchmarkRepoOrganizer createBenchmarkRepoOrganizer() {
		try {
			return new TempFileBenchmarkRepoOrganizer();
		} catch (IOException e) {
			die(e, "Could not create temporary files :(");
			// can not be reached as exit exits
			return null;
		}
	}

	/**
	 * Exists the program after printing an error message.
	 *
	 * @param e the exception
	 * @param message the message to print first
	 */
	private static void die(Throwable e, String message) {
		System.err.println(message);
		System.err.println("A more detailed message: " + e.getMessage());
		System.err.println();
		System.err.println("And here is the full stack trace if you want to report the error:");
		e.printStackTrace(System.err);
		exiter.exit(1);
		throw new IllegalStateException("Exiter did not exit!");
	}

	@JsonIgnoreProperties("_comment")
	private static class RunnerConfigPojo {

		private URI serverUrl;

		private String runnerToken;

		private String runnerName;

		@JsonCreator
		public RunnerConfigPojo(URI serverUrl, String runnerToken, String runnerName) {
			this.serverUrl = Objects.requireNonNull(serverUrl, "serverUrl can not be null!");
			this.runnerToken = Objects.requireNonNull(runnerToken, "runnerToken can not be null!");
			this.runnerName = Objects.requireNonNull(runnerName, "runnerName can not be null!");
		}

		public URI getServerUrl() {
			return serverUrl;
		}

		public String getRunnerToken() {
			return runnerToken;
		}

		public String getRunnerName() {
			return runnerName;
		}
	}
}
