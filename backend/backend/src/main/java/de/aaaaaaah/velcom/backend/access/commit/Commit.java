package de.aaaaaaah.velcom.backend.access.commit;

import de.aaaaaaah.velcom.backend.access.repo.Repo;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.access.repo.exception.NoSuchRepoException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A git commit.
 */
public class Commit {

	private final CommitAccess commitAccess;
	private final RepoAccess repoAccess;

	private final RepoId repoId;
	private final CommitHash hash;
	private final Collection<CommitHash> parentHashes;
	private final String author;
	private final Instant authorDate;
	private final String committer;
	private final Instant committerDate;
	private final String message;

	Commit(CommitAccess commitAccess, RepoAccess repoAccess, RepoId repoId, CommitHash hash,
		Collection<CommitHash> parentHashes, String author, Instant authorDate, String committer,
		Instant committerDate, String message) {

		this.commitAccess = commitAccess;
		this.repoAccess = repoAccess;

		this.repoId = repoId;
		this.hash = hash;
		this.parentHashes = List.copyOf(parentHashes);
		this.author = author;
		this.authorDate = authorDate;
		this.committer = committer;
		this.committerDate = committerDate;
		this.message = message;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public Repo getRepo() throws NoSuchRepoException {
		return repoAccess.getRepo(repoId);
	}

	public CommitHash getHash() {
		return hash;
	}

	public Collection<CommitHash> getParentHashes() {
		return parentHashes;
	}

	public Collection<Commit> getParents() throws CommitAccessException {
		return commitAccess.getCommits(repoId, parentHashes);
	}

	public String getAuthor() {
		return author;
	}

	public Instant getAuthorDate() {
		return authorDate;
	}

	public String getCommitter() {
		return committer;
	}

	public Instant getCommitterDate() {
		return committerDate;
	}

	public String getMessage() {
		return message;
	}

	/**
	 * @return whether the system has seen this commit before
	 */
	public boolean isKnown() {
		return commitAccess.isKnown(repoId, hash);
	}

	public BenchmarkStatus getBenchmarkStatus() {
		return commitAccess.getBenchmarkStatus(repoId, hash);
	}

	@Override
	public String toString() {
		return "Commit{" +
			"repoId=" + repoId +
			", hash=" + hash +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Commit commit = (Commit) o;
		return repoId.equals(commit.repoId) &&
			hash.equals(commit.hash);
	}

	@Override
	public int hashCode() {
		return Objects.hash(repoId, hash);
	}

}
