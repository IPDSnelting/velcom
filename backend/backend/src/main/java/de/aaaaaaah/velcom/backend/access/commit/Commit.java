package de.aaaaaaah.velcom.backend.access.commit;

import de.aaaaaaah.velcom.backend.access.repo.Repo;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

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

	// FIXME: 15.12.19 I wanna be the most package private constructor in the world
	public Commit(CommitAccess commitAccess, RepoAccess repoAccess, RepoId repoId, CommitHash hash,
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

	public Repo getRepo() {
		return repoAccess.getRepo(repoId);
	}

	public CommitHash getHash() {
		return hash;
	}

	public Collection<CommitHash> getParentHashes() {
		return parentHashes;
	}

	public Collection<Commit> getParents() {
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
}
