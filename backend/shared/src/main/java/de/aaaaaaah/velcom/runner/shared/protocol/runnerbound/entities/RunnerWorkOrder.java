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

	/**
	 * Creates a new {@link RunnerWorkOrder}.
	 *
	 * @param repoId the id of the repo that should be benchmarked
	 * @param commitHash the commit hash
	 */
	@JsonCreator
	public RunnerWorkOrder(UUID repoId, String commitHash) {
		this.repoId = repoId;
		this.commitHash = commitHash;
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

	@Override
	public String toString() {
		return "RunnerWorkOrder{" +
			"repoId=" + repoId +
			", commitHash='" + commitHash + '\'' +
			'}';
	}
}
