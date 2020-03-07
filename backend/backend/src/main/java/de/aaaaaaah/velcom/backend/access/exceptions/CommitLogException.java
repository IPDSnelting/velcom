package de.aaaaaaah.velcom.backend.access.exceptions;

import de.aaaaaaah.velcom.backend.access.entities.BranchName;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import java.util.Collection;

public class CommitLogException extends RuntimeException {

	private final RepoId repoId;
	private final Collection<BranchName> branches;

	public CommitLogException(RepoId repoId, Collection<BranchName> branches, Throwable cause) {
		super("failed to execute git log on " + repoId + ": " + branches, cause);
		this.repoId = repoId;
		this.branches = branches;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public Collection<BranchName> getBranches() {
		return branches;
	}

}
