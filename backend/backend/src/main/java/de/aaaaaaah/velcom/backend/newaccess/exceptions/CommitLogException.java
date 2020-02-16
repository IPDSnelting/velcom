package de.aaaaaaah.velcom.backend.newaccess.exceptions;

import de.aaaaaaah.velcom.backend.newaccess.entities.BranchName;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
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
