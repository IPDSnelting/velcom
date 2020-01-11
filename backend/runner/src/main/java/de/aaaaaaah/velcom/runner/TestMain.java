package de.aaaaaaah.velcom.runner;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.entity.TempFileBenchmarkRepoOrganizer;
import de.aaaaaaah.velcom.runner.entity.execution.BenchmarkscriptWorkExecutor;
import de.aaaaaaah.velcom.runner.exceptions.ConnectionException;
import de.aaaaaaah.velcom.runner.protocol.WebsocketListener;
import de.aaaaaaah.velcom.runner.shared.protocol.serialization.SimpleJsonSerializer;
import de.aaaaaaah.velcom.runner.state.RunnerStateMachine;
import java.net.URI;

/**
 * A temporary test class to start the runner.
 */
public class TestMain {

	/**
	 * Starts the runner and connects to a hardcoded server using a hardcoded password.
	 *
	 * @param args the command line arguments
	 * @throws Exception if something bad happens
	 */
	public static void main(String[] args) throws Exception {
		WebsocketListener websocketListener = new WebsocketListener();

		RunnerConfiguration configuration = new RunnerConfiguration(
			new SimpleJsonSerializer(),
			"Test runner",
			"Correct-Horse_Battery Staple",
			websocketListener,
			new RunnerStateMachine(),
			new BenchmarkscriptWorkExecutor(),
			URI.create("ws://localhost:3546/runner-connector"),
			new TempFileBenchmarkRepoOrganizer()
		);
		websocketListener.setConfiguration(configuration);

		try {
			websocketListener.connect();
		} catch (ConnectionException e) {
			System.err.println(
				" _   _       _                     _ _          _   _____                    _   _"
					+ "                  __\n"
					+ "| | | |_ __ | |__   __ _ _ __   __| | | ___  __| | | ____|_  _____ ___ _ __ "
					+ "| |_(_) ___  _ __    _ / /\n"
					+ "| | | | '_ \\| '_ \\ / _` | '_ \\ / _` | |/ _ \\/ _` | |  _| \\ \\/ / __/ _ "
					+ "\\ '_ \\| __| |/ _ \\| '_ \\  (_) | \n"
					+ "| |_| | | | | | | | (_| | | | | (_| | |  __/ (_| | | |___ >  < (_|  __/ |_) "
					+ "| |_| | (_) | | | |  _| | \n"
					+ " \\___/|_| |_|_| |_|\\__,_|_| |_|\\__,_|_|\\___|\\__,_| |_____/_/\\_\\___\\_"
					+ "__| .__/ \\__|_|\\___/|_| |_| (_) | \n"
					+ "                                                                      |_|   "
					+ "                       \\_\\\n"
			);
			e.printStackTrace();
			System.err.println("\n\nExiting...");
			System.exit(1);
		}

		System.out.println("Startup completed");
		System.in.read();
	}
}
