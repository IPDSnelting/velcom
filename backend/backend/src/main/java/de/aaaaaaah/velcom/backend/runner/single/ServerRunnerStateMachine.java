package de.aaaaaaah.velcom.backend.runner.single;

import de.aaaaaaah.velcom.backend.access.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.runner.Dispatcher;
import de.aaaaaaah.velcom.backend.runner.single.state.RunnerWorkSender;
import de.aaaaaaah.velcom.backend.util.CheckedConsumer;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.StatusCodeMappings;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RequestResults;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RequestStatus;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.ResetOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.UpdateBenchmarkRepoOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main runner state machine.
 */
public class ServerRunnerStateMachine {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerRunnerStateMachine.class);

	private final Dispatcher dispatcher;
	private ActiveRunnerInformation runnerInformation;
	private final AtomicBoolean registeredWithDispatcher;

	public ServerRunnerStateMachine(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
		this.registeredWithDispatcher = new AtomicBoolean(false);
	}

	/**
	 * Called when a connection was established.
	 *
	 * @param information the information about the runner
	 */
	public void onConnectionOpened(ActiveRunnerInformation information) {
		this.runnerInformation = information;
	}

	/**
	 * Calle when a message was received.
	 *
	 * @param type the type of the message
	 * @param entity the sent entity
	 */
	public void onMessageReceived(String type, SentEntity entity) {
		switch (type) {
			case "RunnerInformation": {
				RunnerInformation runnerInformation = (RunnerInformation) entity;
				this.runnerInformation.setRunnerInformation(runnerInformation);
				if (runnerInformation.getResults() != null) {
					requestResults();
				}

				LOGGER.info("Got info: " + runnerInformation);

				if (!registeredWithDispatcher.getAndSet(true)) {
					dispatcher.addRunner(this.runnerInformation);
				}

				break;
			}
			case "BenchmarkResults": {
				dispatcher.onResultsReceived(runnerInformation, (BenchmarkResults) entity);
				break;
			}
			case "BenchmarkDone": {
				requestResults();
				runnerInformation.clearCurrentCommit();
				break;
			}
			case "ReadyForWork": {
				dispatcher.runnerAvailable(runnerInformation);
				break;
			}
		}
	}

	private void requestResults() {
		try {
			runnerInformation.getConnectionManager().sendEntity(new RequestResults());
		} catch (IOException e) {
			LOGGER.info("Failed to send requests results", e);
			runnerInformation.getConnectionManager().disconnect();
		}
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
		LOGGER.debug("Sending work {} to {}", workOrder, runnerInformation.getRunnerInformation());
		markAsMyCommit(commit);
		runnerInformation.getConnectionManager().sendEntity(workOrder);
		try (var out = runnerInformation.getConnectionManager().createBinaryOutputStream()) {
			writer.accept(out);
		} catch (Exception e) {
			throw e;
		}
	}

	public void requestStatusUpdate() {
		LOGGER.info("Requesting status update");
		try {
			runnerInformation.getConnectionManager().sendEntity(new RequestStatus());
		} catch (IOException e) {
			LOGGER.info("Could not request status, disconnecting runner");
			runnerInformation.getConnectionManager().disconnect(
				StatusCodeMappings.SERVER_INITIATED_DISCONNECT, "Request status failed"
			);
		}
	}

	/**
	 * Dispatches a commit, updating the bench repo and performing related tasks.
	 *
	 * @param commit the commit
	 * @param access the repo write access
	 * @throws de.aaaaaaah.velcom.backend.access.exceptions.ArchiveFailedPermanently if the archiving
	 * 	fails
	 * @throws IOException if any other network problem is detected
	 */
	public void dispatchCommit(Commit commit, RepoWriteAccess access) throws IOException {
		runnerInformation.getRunnerInformation().ifPresent(information -> {
			runnerInformation.setRunnerInformation(new RunnerInformation(
				information.getName(),
				information.getOperatingSystem(),
				information.getCoreCount(),
				information.getAvailableMemory(),
				RunnerStatusEnum.PREPARING_WORK,
				information.getCurrentBenchmarkRepoHash().orElse(null),
				information.getResults()
			));
		});
		new RunnerWorkSender(commit, access).sendWork(runnerInformation);
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
	}
}
