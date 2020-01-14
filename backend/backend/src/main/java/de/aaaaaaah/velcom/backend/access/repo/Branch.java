package de.aaaaaaah.velcom.backend.access.repo;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccessException;
import de.aaaaaaah.velcom.backend.access.repo.exception.RepoAccessException;
import java.util.Objects;

/**
 * A branch inside a git repository.
 */
public class Branch {

	private final RepoAccess repoAccess;
	private final CommitAccess commitAccess;

	private final RepoId repoId;
	private final BranchName name;

	Branch(RepoAccess repoAccess, CommitAccess commitAccess, RepoId repoId, BranchName name) {
		this.repoAccess = repoAccess;
		this.commitAccess = commitAccess;

		this.repoId = repoId;
		this.name = name;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public Repo getRepo() {
		return repoAccess.getRepo(repoId);
	}

	public BranchName getName() {
		return name;
	}

	public Commit getCommit() throws CommitAccessException, RepoAccessException {
		return commitAccess.getCommit(repoId, repoAccess.getLatestCommitHash(this));
	}

	/**
	 * @return whether the branch is tracked
	 */
	public boolean isTracked() {
		return repoAccess.isBranchTracked(repoId, name);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Branch branch = (Branch) o;
		return Objects.equals(repoId, branch.repoId) &&
			Objects.equals(name, branch.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(repoId, name);
	}

	@Override
	public String toString() {
		return "Branch{" +
			"repoAccess=" + repoAccess +
			", repoId=" + repoId +
			", name=" + name +
			'}';
	}
}
