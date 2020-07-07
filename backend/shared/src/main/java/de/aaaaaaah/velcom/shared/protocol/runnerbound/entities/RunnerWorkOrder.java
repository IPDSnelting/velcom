package de.aaaaaaah.velcom.shared.protocol.runnerbound.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.aaaaaaah.velcom.shared.protocol.SentEntity;
import java.util.Objects;
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
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RunnerWorkOrder workOrder = (RunnerWorkOrder) o;
		return Objects.equals(repoId, workOrder.repoId) &&
			Objects.equals(commitHash, workOrder.commitHash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(repoId, commitHash);
	}

	@Override
	public String toString() {
		return "RunnerWorkOrder{" +
			"repoId=" + repoId +
			", commitHash='" + commitHash + '\'' +
			'}';
	}
}
