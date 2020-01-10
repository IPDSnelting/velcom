package de.aaaaaaah.velcom.runner.shared.protocol.runnerbound.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.aaaaaaah.velcom.runner.shared.protocol.SentEntity;
import java.util.UUID;

/**
 * Causes the runner to start fetching work amd contains all needed metadata to start execution.
 */
public class RunnerWorkOrder implements SentEntity {

	private final UUID repoId;
	private final String commitHash;
	private final String remoteUrlIdentifier;

	/**
	 * Creates a new {@link RunnerWorkOrder}.
	 *
	 * @param repoId the id of the repo that should be benchmarked
	 * @param commitHash the commit hash
	 * @param remoteUrlIdentifier an identifier for the remote URL to select a nice directory to
	 */
	@JsonCreator
	public RunnerWorkOrder(UUID repoId, String commitHash, String remoteUrlIdentifier) {
		this.repoId = repoId;
		this.commitHash = commitHash;
		this.remoteUrlIdentifier = remoteUrlIdentifier;
	}

	/**
	 * Returns the id of the repo to benchmark.
	 *
	 * @return the id of the repo to benchmark
	 */
	public UUID getRepoId() {
		return repoId;
	}

	/**
	 * Returns the commit hash to test.
	 *
	 * @return the commit hash
	 */
	public String getCommitHash() {
		return commitHash;
	}

	/**
	 * Returns an identifier for the remote URL to select a nice directory to clone to.
	 *
	 * @return an identifier for the remote URL to select a nice directory to clone to
	 */
	public String getRemoteUrlIdentifier() {
		return remoteUrlIdentifier;
	}

	@Override
	public String toString() {
		return "RunnerWorkOrder{" +
			"repoId=" + repoId +
			", commitHash='" + commitHash + '\'' +
			", remoteUrlIdentifier='" + remoteUrlIdentifier + '\'' +
			'}';
	}
}
