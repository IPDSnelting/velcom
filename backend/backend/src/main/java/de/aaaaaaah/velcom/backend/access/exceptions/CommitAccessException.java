package de.aaaaaaah.velcom.backend.access.exceptions;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;

public class CommitAccessException extends RuntimeException {

	private final RepoId repoId;
	private final CommitHash commitHash;

	public CommitAccessException(RepoId repoId, CommitHash commitHash) {
		super("failed to access commit in repo: " + repoId + ", " + commitHash);
		this.repoId = repoId;
		this.commitHash = commitHash;
	}

	public CommitAccessException(String message, Throwable cause, RepoId repoId,
		CommitHash commitHash) {

		super(message, cause);
		this.repoId = repoId;
		this.commitHash = commitHash;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public CommitHash getCommitHash() {
		return commitHash;
	}
}
