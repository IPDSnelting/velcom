package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

public class JsonCommit {

	private final UUID repoId;
	private final String hash;
	private final boolean tracked;
	private final List<JsonCommitDescription> trackedParents;
	private final List<JsonCommitDescription> untrackedParents;
	private final List<JsonCommitDescription> trackedChildren;
	private final List<JsonCommitDescription> untrackedChildren;
	private final String author;
	private final long authorDate;
	private final String committer;
	private final long committerDate;
	private final String summary;
	@Nullable
	private final String message;
	private final List<JsonRunDescription> runs;

	public JsonCommit(UUID repoId, String hash, boolean tracked,
		List<JsonCommitDescription> trackedParents, List<JsonCommitDescription> untrackedParents,
		List<JsonCommitDescription> trackedChildren, List<JsonCommitDescription> untrackedChildren,
		String author, long authorDate, String committer, long committerDate, String summary,
		@Nullable String message, List<JsonRunDescription> runs) {

		this.repoId = repoId;
		this.hash = hash;
		this.tracked = tracked;
		this.trackedParents = trackedParents;
		this.untrackedParents = untrackedParents;
		this.trackedChildren = trackedChildren;
		this.untrackedChildren = untrackedChildren;
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

	public boolean isTracked() {
		return tracked;
	}

	public List<JsonCommitDescription> getTrackedParents() {
		return trackedParents;
	}

	public List<JsonCommitDescription> getUntrackedParents() {
		return untrackedParents;
	}

	public List<JsonCommitDescription> getTrackedChildren() {
		return trackedChildren;
	}

	public List<JsonCommitDescription> getUntrackedChildren() {
		return untrackedChildren;
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
