package de.aaaaaaah.velcom.runner.cli;

import de.aaaaaaah.velcom.runner.cli.RunnerCliSpec_Parser.HelpRequested;
import de.aaaaaaah.velcom.runner.cli.RunnerCliSpec_Parser.ParseResult;
import de.aaaaaaah.velcom.runner.cli.RunnerCliSpec_Parser.ParsingFailed;
import de.aaaaaaah.velcom.runner.cli.RunnerCliSpec_Parser.ParsingSuccess;
import de.aaaaaaah.velcom.runner.entity.BenchmarkRepoOrganizer;
import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.entity.TempFileBenchmarkRepoOrganizer;
import de.aaaaaaah.velcom.runner.entity.execution.BenchmarkscriptWorkExecutor;
import de.aaaaaaah.velcom.runner.exceptions.ConnectionException;
import de.aaaaaaah.velcom.runner.protocol.WebsocketListener;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.SimpleJsonSerializer;
import de.aaaaaaah.velcom.runner.state.RunnerStateMachine;
import java.io.IOException;

/**
 * The runner main class.
 */
public class RunnerMain {

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
			System.exit(0);
		} else if (parseResult instanceof ParsingFailed) {
			System.err.println("Error: " + ((ParsingFailed) parseResult).getError().getMessage());
			System.err.println();
			System.err.println();
			new RunnerCliSpec_Parser().printOnlineHelp(System.err);
			System.exit(0);
		}
		RunnerCliSpec cliSpec = ((ParsingSuccess) parseResult).getResult();

		WebsocketListener websocketListener = new WebsocketListener();

		RunnerConfiguration runnerConfiguration = new RunnerConfiguration(
			new SimpleJsonSerializer(),
			cliSpec.runnerName(),
			cliSpec.accessToken(),
			websocketListener,
			new RunnerStateMachine(),
			new BenchmarkscriptWorkExecutor(),
			cliSpec.serverUrl(),
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
			"I will keep trying to connect and reconnect until you kill me (SIGTERM)"
		);

		// Initial connect
		try {
			websocketListener.connect();
		} catch (ConnectionException e) {
			System.err.println("Initial server connection failed. Consider checking your details!");
			System.err.println("Message: " + e.getMessage());
		}

		// Graceful shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(websocketListener::disconnect));

		// That was my goal.
		//noinspection InfiniteLoopStatement
		while (true) {
			Thread.sleep(10_000);
		}
	}

	private static BenchmarkRepoOrganizer createBenchmarkRepoOrganizer() {
		try {
			return new TempFileBenchmarkRepoOrganizer();
		} catch (IOException e) {
			System.err.println("Could not create temporary files :(");
			System.err.println("A more detailed message: " + e.getMessage());
			System.err.println();
			System.err.println("And here is the full stack trace if you want to report the error:");
			e.printStackTrace(System.err);
			System.exit(1);
			// can not be reached as exit exits
			return null;
		}
	}

}
