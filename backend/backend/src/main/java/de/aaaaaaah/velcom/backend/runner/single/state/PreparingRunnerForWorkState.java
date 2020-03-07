package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.newaccess.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.runner.shared.RunnerStatusEnum;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import de.aaaaaaah.velcom.runner.shared.protocol.StatusCodeMappings;
import de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.BenchmarkResults;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.RunnerInformation;
import de.aaaaaaah.velcom.runner.shared.protocol.serverbound.entities.WorkReceived;
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
