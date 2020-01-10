package de.aaaaaaah.velcom.backend.access.queue;

import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.time.Instant;

/**
 * A task is a commit that has been entered into the queue at a specific point in time.
 */
public class Task {

	private final RepoId repoId;
	private final CommitHash commitHash;
	private final Instant insertTime;

	Task(RepoId repoId, CommitHash commitHash, Instant insertTime) {
		this.repoId = repoId;
		this.commitHash = commitHash;
		this.insertTime = insertTime;
	}

	public Task(RepoId repoId, CommitHash commitHash) {
		this(repoId, commitHash, Instant.now());
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public CommitHash getCommitHash() {
		return commitHash;
	}

	public Instant getInsertTime() {
		return insertTime;
	}
}
