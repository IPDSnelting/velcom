package de.aaaaaaah.velcom.backend.access.commit;

import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.util.NoSuchElementException;

/**
 * This exception is thrown whenever an invalid combination of {@link RepoId} and {@link CommitHash}
 * is used.
 */
public class NoSuchCommitException extends NoSuchElementException {

	private final RepoId repoId;
	private final CommitHash commitHash;

	public NoSuchCommitException(RepoId repoId, CommitHash commitHash) {
		this.repoId = repoId;
		this.commitHash = commitHash;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public CommitHash getCommitHash() {
		return commitHash;
	}

	@Override
	public String getMessage() {
		return "no commit in repo " + repoId + " with hash " + commitHash;
	}
}
