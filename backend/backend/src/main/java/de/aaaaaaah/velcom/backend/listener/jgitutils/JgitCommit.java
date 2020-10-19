package de.aaaaaaah.velcom.backend.listener.jgitutils;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import java.time.Instant;
import java.util.Set;

public class JgitCommit {

	private final CommitHash hash;
	private final Set<CommitHash> parentHashes;
	private final String author;
	private final Instant authorDate;
	private final String committer;
	private final Instant committerDate;
	private final String message;

	public JgitCommit(CommitHash hash, Set<CommitHash> parentHashes, String author,
		Instant authorDate, String committer, Instant committerDate, String message) {

		this.hash = hash;
		this.parentHashes = parentHashes;
		this.author = author;
		this.authorDate = authorDate;
		this.committer = committer;
		this.committerDate = committerDate;
		this.message = message;
	}

	public CommitHash getHash() {
		return hash;
	}

	public Set<CommitHash> getParentHashes() {
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
