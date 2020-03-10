package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.access.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.exceptions.ArchiveFailedPermanently;
import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.StatusCodeMappings;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.WorkReceived;
import de.aaaaaaah.velcom.runner.shared.util.StringOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepares the runner for work.
 */
public class PreparingRunnerForWorkState implements RunnerState {

	private static final Logger LOGGER = LoggerFactory.getLogger(PreparingRunnerForWorkState.class);

	private Commit commit;
	private RepoWriteAccess repoAccess;

	/**
	 * Creates a new state.
	 *
	 * @param commit the commit to send
	 * @param repoAccess the repo access to query
	 */
	public PreparingRunnerForWorkState(Commit commit, RepoWriteAccess repoAccess) {
		this.commit = commit;
		this.repoAccess = repoAccess;
	}

	@Override
	public RunnerStatusEnum getStatus() {
		return RunnerStatusEnum.PREPARING_WORK;
	}

	@Override
	public void onSelected(ActiveRunnerInformation information) {
		if (information.getRunnerInformation().isEmpty()) {
			LOGGER.warn("Tried to dispatch a commit to a runner without information! ({})", commit);
			information.getConnectionManager().disconnect();
			return;
		}
		Instant start = Instant.now();

		information.getRunnerStateMachine().markAsMyCommit(commit);

		String runnerBenchmarkCommitHash = information.getRunnerInformation()
			.get()
			.getCurrentBenchmarkRepoHash()
			.orElse("");

		try {
			String currentBenchmarkRepoHash = repoAccess.getLatestBenchmarkRepoHash().getHash();

			if (!runnerBenchmarkCommitHash.equals(currentBenchmarkRepoHash)) {
				information.getRunnerStateMachine().sendBenchmarkRepo(
					repoAccess::streamBenchmarkRepoArchive,
					currentBenchmarkRepoHash
				);
			}

			RunnerWorkOrder workOrder = new RunnerWorkOrder(
				commit.getRepoId().getId(), commit.getHash().getHash()
			);

			// Commit was cancelled
			if (information.getCurrentCommit().isEmpty()) {
				return;
			}

			information.getRunnerStateMachine().startWork(
				commit,
				workOrder,
				outputStream -> repoAccess.streamNormalRepoArchive(
					commit.getRepoId(), commit.getHash(), outputStream
				)
			);
		} catch (ArchiveFailedPermanently e) {
			LOGGER.error(
				"Archiving repo failed with a more permanent cause! I am not trying again",
				e
			);
			StringOutputStream stringOutputStream = new StringOutputStream();
			e.printStackTrace(new PrintStream(stringOutputStream));
			information.getRunnerStateMachine().onWorkDone(
				new BenchmarkResults(
					new RunnerWorkOrder(commit.getRepoId().getId(), commit.getHash().getHash()),
					"Failed to archive the repo!\n" + stringOutputStream.getString(),
					start,
					Instant.now()
				)
			);
			try {
				information.getRunnerStateMachine().resetRunner("Dispatch failed permanently");
			} catch (IOException ex) {
				LOGGER.info("Error resetting runner after failed dispatch!", ex);
				information.getConnectionManager().disconnect(
					StatusCodeMappings.SERVER_INITIATED_DISCONNECT, "Failed to reset"
				);
			}
		} catch (Throwable e) {
			LOGGER.info("Dispatching commit not possible", e);
			information.getConnectionManager().disconnect(
				StatusCodeMappings.SERVER_INITIATED_DISCONNECT,
				"Dispatching commit failed"
			);
		}
	}

	@Override
	public RunnerState onMessage(String type, SentEntity entity,
		ActiveRunnerInformation information) {
		if (WorkReceived.class.getSimpleName().equals(type)) {
			return new RunnerWorkingState();
		} else if (BenchmarkResults.class.getSimpleName().equals(type)) {
			information.getRunnerStateMachine().onWorkDone((BenchmarkResults) entity);
			if (information.getCurrentCommit().isEmpty()) {
				return new RunnerIdleState();
			}
			return this;
		} else if (RunnerInformation.class.getSimpleName().equals(type)) {
			information.setRunnerInformation((RunnerInformation) entity);
			return this;
		}
		LOGGER.info("Unknown message type received {} with {}", type, entity);
		information.getConnectionManager().disconnect(
			StatusCodeMappings.SERVER_INITIATED_DISCONNECT,
			"Invalid message received: " + type
		);
		return this;
	}
}
