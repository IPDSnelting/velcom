package de.aaaaaaah.velcom.backend.newaccess.committaccess.exceptions;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;

/**
 * This exception is thrown whenever an invalid combination of {@link RepoId} and {@link CommitHash}
 * is used.
 */
public class NoSuchCommitException extends RuntimeException {

	private final RepoId repoId;
	private final CommitHash commitHash;

	public NoSuchCommitException(Throwable t, RepoId repoId, CommitHash commitHash) {
		super("no commit in repo " + repoId + " with hash " + commitHash, t);

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
