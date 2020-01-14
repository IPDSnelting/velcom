package de.aaaaaaah.velcom.runner.state;

import de.aaaaaaah.velcom.runner.entity.RunnerConfiguration;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.io.IOException;
import java.nio.file.Path;

/**
 * The main state machine for the runner.
 */
public class RunnerStateMachine {

	private RunnerState state;
	private BenchmarkResults lastResults;

	/**
	 * Creates a new state machine.
	 */
	public RunnerStateMachine() {
		this.state = new IdleState();
	}

	/**
	 * A connection was established. Called for reconnects or the first connection.
	 *
	 * @param configuration the runner configuration
	 */
	public void onConnectionEstablished(RunnerConfiguration configuration) {
		doWithErrorAndSwitch(
			() -> {
				System.out.println("Established with " + state.getStatus());
				configuration.getConnectionManager().sendEntity(new RunnerInformation(
					"Test name",
					System.getProperty("os.name")
						+ " " + System.getProperty("os.arch")
						+ " " + System.getProperty("os.version"),
					Runtime.getRuntime().availableProcessors(),
					Runtime.getRuntime().maxMemory(),
					state.getStatus(),
					configuration.getBenchmarkRepoOrganizer().getHeadHash().orElse(null)
				));
				sendResultsIfAny(configuration);
				return state;
			},
			configuration
		);
	}

	/**
	 * Called with the saved path when a file was received.
	 *
	 * @param path the path to the received file. Most likely a temporary directory and deleting it
	 * 	is explicitly allowed.
	 * @param configuration the runner configuration
	 */
	public void onFileReceived(Path path, RunnerConfiguration configuration) {
		doWithErrorAndSwitch(
			() -> state.onFileReceived(path, configuration),
			configuration
		);
	}

	/**
	 * Called when the runner receives some work order from the server.
	 *
	 * @param workOrder the work to complete
	 * @param configuration the runner configuration
	 */
	public void onWorkArrived(RunnerWorkOrder workOrder, RunnerConfiguration configuration) {
		doWithErrorAndSwitch(
			() -> state.onWorkArrived(workOrder, configuration),
			configuration
		);
	}

	/**
	 * Called when the server requests a reset.
	 *
	 * @param reason the reason for the request
	 * @param configuration the runner configuration
	 */
	public void onResetRequested(String reason, RunnerConfiguration configuration) {
		doWithErrorAndSwitch(
			() -> {
				System.out.println("Aborting due to " + reason + "...");
				configuration.getWorkExecutor().abortExecution();
				return new IdleState();
			},
			configuration
		);
	}

	/**
	 * Called when a benchmark was finished.
	 *
	 * @param results the benchmark results
	 * @param configuration the runner configuration
	 */
	public void onWorkDone(BenchmarkResults results, RunnerConfiguration configuration) {
		doWithErrorAndSwitch(
			() -> {
				this.lastResults = results;
				sendResultsIfAny(configuration);
				return new IdleState();
			},
			configuration
		);
	}

	/**
	 * Called when the runner receives a message telling it to update its benchmark repo copy.
	 *
	 * @param newCommitHash the hash of the new head commit
	 * @param configuration the runner configuration
	 */
	public void onUpdateBenchmarkRepo(String newCommitHash, RunnerConfiguration configuration) {
		doWithErrorAndSwitch(() -> new UpdateBenchmarkRepoState(newCommitHash), configuration);
	}


	private void sendResultsIfAny(RunnerConfiguration configuration) throws IOException {
		if (lastResults != null && configuration.getConnectionManager().isConnected()) {
			System.out.println("Sending results...");
			configuration.getConnectionManager().sendEntity(lastResults);
			lastResults = null;
		}
	}

	private void doWithErrorAndSwitch(IOErrorCallable action, RunnerConfiguration configuration) {
		try {
			RunnerState newState = action.run();
			if (newState != state) {
				System.out.println("Switching from " + state + " to " + newState);
				this.state = newState;
				newState.onSelected(configuration);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private interface IOErrorCallable {

		RunnerState run() throws IOException;
	}
}
