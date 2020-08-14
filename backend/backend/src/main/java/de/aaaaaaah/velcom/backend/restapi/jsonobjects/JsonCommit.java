package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

public class JsonCommit {

	private final UUID repoId;
	private final String hash;
	private final List<JsonCommitDescription> parents;
	private final List<JsonCommitDescription> children;
	private final String author;
	private final long authorDate;
	private final String committer;
	private final long committerDate;
	private final String summary;
	@Nullable
	private final String message;
	private final List<JsonRunDescription> runs;

	public JsonCommit(UUID repoId, String hash, List<JsonCommitDescription> parents,
		List<JsonCommitDescription> children, String author, long authorDate, String committer,
		long committerDate, String summary, @Nullable String message, List<JsonRunDescription> runs) {

		this.repoId = repoId;
		this.hash = hash;
		this.parents = parents;
		this.children = children;
		this.author = author;
		this.authorDate = authorDate;
		this.committer = committer;
		this.committerDate = committerDate;
		this.summary = summary;
		this.message = message;
		this.runs = runs;
	}

	public UUID getRepoId() {
		return repoId;
	}

	public String getHash() {
		return hash;
	}

	public List<JsonCommitDescription> getParents() {
		return parents;
	}

	public List<JsonCommitDescription> getChildren() {
		return children;
	}

	public String getAuthor() {
		return author;
	}

	public long getAuthorDate() {
		return authorDate;
	}

	public String getCommitter() {
		return committer;
	}

	public long getCommitterDate() {
		return committerDate;
	}

	public String getSummary() {
		return summary;
	}

	@Nullable
	public String getMessage() {
		return message;
	}

	public List<JsonRunDescription> getRuns() {
		return runs;
	}
}
