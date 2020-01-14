package de.aaaaaaah.velcom.backend.runner.single;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.runner.single.state.RunnerDisconnectedState;
import de.aaaaaaah.velcom.backend.runner.single.state.RunnerIdleState;
import de.aaaaaaah.velcom.backend.runner.single.state.RunnerInitializingState;
import de.aaaaaaah.velcom.backend.runner.single.state.RunnerState;
import de.aaaaaaah.velcom.backend.util.CheckedConsumer;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.ResetOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.UpdateBenchmarkRepoOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.io.IOException;
import java.io.OutputStream;

/**
 * The main runner state machine.
 */
public class ServerRunnerStateMachine {

	private ActiveRunnerInformation runnerInformation;
	private RunnerState state;

	/**
	 * Creates a new server-side state machine for a single runner.
	 */
	public ServerRunnerStateMachine() {
		this.state = new RunnerDisconnectedState();
	}

	/**
	 * @return the current state
	 */
	public RunnerState getState() {
		return state;
	}

	/**
	 * Called when a connection was established.
	 *
	 * @param information the information about the runner
	 */
	public void onConnectionOpened(ActiveRunnerInformation information) {
		System.out.println("Connection opened: " + information);
		this.runnerInformation = information;
		switchState(new RunnerInitializingState());
	}

	/**
	 * Calle when a message was received.
	 *
	 * @param type the type of the message
	 * @param entity the sent entity
	 */
	public void onMessageReceived(String type, SentEntity entity) {
		RunnerState previousState = state;
		RunnerState newState = state.onMessage(type, entity, runnerInformation);
		if (newState != state && previousState == state) {
			switchState(newState);
		}
	}

	private void switchState(RunnerState newState) {
		System.out.println("Switching from " + state + " to " + newState);
		state = newState;
		newState.onSelected(runnerInformation);
	}

	/**
	 * Called when the runner has completed its work.
	 *
	 * @param results the benchmark results
	 */
	public void onWorkDone(BenchmarkResults results) {
		runnerInformation.setResults(results);
		runnerInformation.setCurrentCommit(null);
	}

	/**
	 * Resets the runner.
	 *
	 * @param reason the reason for the reset
	 * @throws IOException if an error occurs
	 */
	public void resetRunner(String reason) throws IOException {
		runnerInformation.getConnectionManager().sendEntity(new ResetOrder(reason));
		runnerInformation.setCurrentCommit(null);
		switchState(new RunnerIdleState());
	}

	/**
	 * Starts the work.
	 *
	 * @param commit the commit
	 * @param workOrder the work order
	 * @param writer writes a repo binary out to the client
	 * @throws IOException if an error occurs
	 */
	public void startWork(Commit commit, RunnerWorkOrder workOrder,
		CheckedConsumer<OutputStream, IOException> writer)
		throws IOException {
		if (runnerInformation.getState() != RunnerStatusEnum.IDLE) {
			throw new IllegalStateException(
				"Invalid state, runner is in " + runnerInformation.getState()
			);
		}
		System.out.println("Sending work: " + workOrder);
		runnerInformation.setCurrentCommit(commit);
		runnerInformation.getConnectionManager().sendEntity(workOrder);
		try (var out = runnerInformation.getConnectionManager().createBinaryOutputStream()) {
			writer.accept(out);
		} catch (Exception e) {
			// TODO: 12.01.20 Make nicer catch
			throw new IOException(e);
		}
	}

	/**
	 * Sends an updated repository to the runner.
	 *
	 * @param writer writes a repo binary out to the client
	 * @param repoHeadHash the hash of the head commit
	 * @throws IOException if an error occurs
	 */
	public void sendBenchmarkRepo(CheckedConsumer<OutputStream, IOException> writer,
		String repoHeadHash) throws IOException {
		System.out.println("Sending an updated repo (" + repoHeadHash + ")");
		runnerInformation.getConnectionManager()
			.sendEntity(new UpdateBenchmarkRepoOrder(repoHeadHash));

		try (var out = runnerInformation.getConnectionManager().createBinaryOutputStream()) {
			writer.accept(out);
		} catch (Exception e) {
			// TODO: 12.01.20 Make nicer catch
			throw new IOException(e);
		}

		// TODO: Update the stored repo hash. Could also force a disconnect or re-send the
		//       information?
		runnerInformation.getRunnerInformation()
			.ifPresent(infos -> runnerInformation.setRunnerInformation(
				new RunnerInformation(
					infos.getName(),
					infos.getOperatingSystem(),
					infos.getCoreCount(),
					infos.getAvailableMemory(),
					infos.getRunnerState(),
					repoHeadHash
				)
			));
	}
}
