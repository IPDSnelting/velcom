package de.aaaaaaah.velcom.backend.runner.single.state;

import de.aaaaaaah.velcom.backend.access.RepoWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.exceptions.ArchiveFailedPermanently;
import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import de.aaaaaaah.velcom.shared.protocol.runnerbound.entities.RunnerWorkOrder;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepares the runner for work.
 */
public class RunnerWorkSender {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunnerWorkSender.class);

	private final Commit commit;
	private final RepoWriteAccess repoAccess;

	/**
	 * Creates a new state.
	 *
	 * @param commit the commit to send
	 * @param repoAccess the repo access to query
	 */
	public RunnerWorkSender(Commit commit, RepoWriteAccess repoAccess) {
		this.commit = commit;
		this.repoAccess = repoAccess;
	}

	/**
	 * Sends work to a runner, throwing an exception if archiving fails.
	 *
	 * @param information the runner information
	 * @throws ArchiveFailedPermanently if the archiving fails
	 * @throws IOException if any other network problem is detected
	 */
	public void sendWork(ActiveRunnerInformation information) throws IOException {
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
	}

	private String archiveRepoErrorHeader() {
		String errorMessageHeader =
			"##         Failed to archive the repo!        ##\n"
				+ "## This message is not pretty, but I tried :( ##\n"
				+ "## It is a stacktrace that hopefully includes ##\n"
				+ "##               the git error.               ##";
		String paddingHashString = "#".repeat(48);
		errorMessageHeader =
			paddingHashString + "\n" + errorMessageHeader + "\n" + paddingHashString + "\n\n";
		return errorMessageHeader;
	}
}
