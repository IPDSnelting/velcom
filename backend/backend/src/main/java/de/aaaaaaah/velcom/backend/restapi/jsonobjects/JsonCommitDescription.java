package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.Commit;
import java.util.UUID;

public class JsonCommitDescription {

	private final UUID repoId;
	private final String hash;
	private final String author;
	private final long authorDate;
	private final String summary;

	public JsonCommitDescription(UUID repoId, String hash, String author, long authorDate,
		String summary) {

		this.repoId = repoId;
		this.hash = hash;
		this.author = author;
		this.authorDate = authorDate;
		this.summary = summary;
	}

	/**
	 * Utility function to create a {@link JsonCommitDescription} directly from a {@link Commit}.
	 *
	 * @param commit the commit to use
	 * @return a new description created from the commit
	 */
	public static JsonCommitDescription fromCommit(Commit commit) {
		return new JsonCommitDescription(
			commit.getRepoId().getId(),
			commit.getHash().getHash(),
			commit.getAuthor(),
			commit.getAuthorDate().getEpochSecond(),
			commit.getSummary()
		);
	}

	public UUID getRepoId() {
		return repoId;
	}

	public String getHash() {
		return hash;
	}

	public String getAuthor() {
		return author;
	}

	public long getAuthorDate() {
		return authorDate;
	}

	public String getSummary() {
		return summary;
	}
}
