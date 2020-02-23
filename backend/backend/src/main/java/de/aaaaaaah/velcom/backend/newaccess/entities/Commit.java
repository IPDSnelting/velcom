package de.aaaaaaah.velcom.backend.newaccess.entities;

import java.time.Instant;
import java.util.Collection;

public class Commit {

	private final RepoId repoId;
	private final CommitHash hash;
	private final Collection<CommitHash> parentHashes;
	private final String author;
	private final Instant authorDate;
	private final String committer;
	private final Instant committerDate;
	private final String message;

	public Commit(RepoId repoId, CommitHash hash, Collection<CommitHash> parentHashes,
		String author, Instant authorDate, String committer, Instant committerDate,
		String message) {

		this.repoId = repoId;
		this.hash = hash;
		this.parentHashes = parentHashes;
		this.author = author;
		this.authorDate = authorDate;
		this.committer = committer;
		this.committerDate = committerDate;
		this.message = message;
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public CommitHash getHash() {
		return hash;
	}

	public Collection<CommitHash> getParentHashes() {
		return parentHashes;
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
}
