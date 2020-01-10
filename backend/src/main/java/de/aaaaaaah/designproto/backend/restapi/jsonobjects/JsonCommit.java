package de.aaaaaaah.designproto.backend.restapi.jsonobjects;

import de.aaaaaaah.designproto.backend.access.commit.Commit;
import de.aaaaaaah.designproto.backend.access.commit.CommitHash;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A helper class for serialization representing a commit.
 */
public class JsonCommit {

	private final UUID repoId;
	private final String hash;
	private final Collection<String> parents;
	private final String author;
	private final Instant authorDate;
	private final String committer;
	private final Instant committerDate;
	private final String message;

	public JsonCommit(Commit commit) {
		repoId = commit.getRepoId().getId();
		hash = commit.getHash().getHash();

		parents = commit.getParentHashes().stream()
			.map(CommitHash::getHash)
			.collect(Collectors.toUnmodifiableList());

		author = commit.getAuthor();
		authorDate = commit.getAuthorDate();
		committer = commit.getCommitter();
		committerDate = commit.getCommitterDate();
		message = commit.getMessage();
	}

	public UUID getRepoId() {
		return repoId;
	}

	public String getHash() {
		return hash;
	}

	public Collection<String> getParents() {
		return parents;
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
