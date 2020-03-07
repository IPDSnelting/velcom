package de.aaaaaaah.velcom.backend.runner.single;

import de.aaaaaaah.velcom.backend.access.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.runner.single.state.PreparingRunnerForWorkState;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main runner state machine.
 */
public class ServerRunnerStateMachine {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerRunnerStateMachine.class);

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
		// Reference comparison is wanted here! Even if a new state of the same type is returned
		// we want to init it
		if (newState != state && previousState == state) {
			switchState(newState);
		}
	}

	private void switchState(RunnerState newState) {
		LOGGER.debug("Switching runner state from {} to {}", state, newState);
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

		// Clear the commit if we got results for it
		runnerInformation.getCurrentCommit().ifPresent(commit -> {
			RunnerWorkOrder order = results.getWorkOrder();
			if (!commit.getRepoId().getId().equals(order.getRepoId())) {
				return;
			}
			if (!commit.getHash().getHash().equals(order.getCommitHash())) {
				return;
			}
			runnerInformation.clearCurrentCommit();
		});
	}

	/**
	 * Resets the runner.
	 *
	 * @param reason the reason for the reset
	 * @throws IOException if an error occurs
	 */
	public void resetRunner(String reason) throws IOException {
		runnerInformation.getConnectionManager().sendEntity(new ResetOrder(reason));
		runnerInformation.clearCurrentCommit();
	}

	/**
	 * Starts the work. Can only be called when the state is PREPARING_WORK.
	 *
	 * <p><br><strong>Please use {@link #dispatchCommit(Commit, RepoWriteAccess)} if you just want
	 * to start work on a commit.</strong>
	 *
	 * @param commit the commit
	 * @param workOrder the work order
	 * @param writer writes a repo binary out to the client
	 * @throws IOException if an error occurs
	 */
	public void startWork(Commit commit, RunnerWorkOrder workOrder,
		CheckedConsumer<OutputStream, IOException> writer)
		throws IOException {
		if (runnerInformation.getState() != RunnerStatusEnum.PREPARING_WORK) {
			throw new IllegalStateException(
				"Invalid state, runner is in " + runnerInformation.getState()
			);
		}
		LOGGER.debug("Sending work {} to {}", workOrder, runnerInformation.getRunnerInformation());
		markAsMyCommit(commit);
		runnerInformation.getConnectionManager().sendEntity(workOrder);
		try (var out = runnerInformation.getConnectionManager().createBinaryOutputStream()) {
			writer.accept(out);
		} catch (Exception e) {
			// TODO: 12.01.20 Make nicer catch
			throw new IOException(e);
		}
	}

	/**
	 * Dispatches a commit, updating the bench repo and performing related tasks.
	 *
	 * @param commit the commit
	 * @param access the repo write access
	 */
	public void dispatchCommit(Commit commit, RepoWriteAccess access) {
		switchState(new PreparingRunnerForWorkState(commit, access));
	}

	/**
	 * Goes back to the idle state.
	 */
	public void backToIdle() {
		if (getState().getStatus() != RunnerStatusEnum.IDLE) {
			switchState(new RunnerIdleState());
		}
	}

	/**
	 * Marks a given commit as the one this runner is working on.
	 *
	 * @param commit the commit or null if none
	 */
	public void markAsMyCommit(Commit commit) {
		runnerInformation.setCurrentCommit(commit);
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
		LOGGER.debug(
			"Updating benchmark repo for {} to {}",
			runnerInformation.getRunnerInformation(), repoHeadHash
		);
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
