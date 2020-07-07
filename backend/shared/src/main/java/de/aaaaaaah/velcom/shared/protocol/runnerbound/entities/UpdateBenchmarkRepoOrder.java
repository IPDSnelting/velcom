package de.aaaaaaah.velcom.shared.protocol.runnerbound.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import de.aaaaaaah.velcom.shared.protocol.SentEntity;

/**
 * Informs the runner that it should update its local benchmark script repository.
 */
public class UpdateBenchmarkRepoOrder implements SentEntity {

	private String commitHash;

	/**
	 * Creates a new update order.
	 *
	 * @param commitHash the new updated hash of the benchmark repo
	 */
	@JsonCreator
	public UpdateBenchmarkRepoOrder(String commitHash) {
		this.commitHash = commitHash;
	}

	/**
	 * Returns the new updated hash of the benchmark repo.
	 *
	 * @return the new updated hash of the benchmark repo
	 */
	public String getCommitHash() {
		return commitHash;
	}

	@Override
	public String toString() {
		return "UpdateBenchmarkRepoOrder{" +
			"commitHash='" + commitHash + '\'' +
			'}';
	}
}
