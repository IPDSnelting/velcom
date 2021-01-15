package de.aaaaaaah.velcom.backend.access.committaccess.entities;

import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Pair;
import java.time.Instant;
import java.util.Optional;

/**
 * A git commit. For a version that includes parent and child hashes, see {@link FullCommit}.
 */
public class Commit {

	private final RepoId repoId;
	private final CommitHash hash;
	private final boolean reachable;
	private final boolean tracked;
	private final String author;
	private final Instant authorDate;
	private final String committer;
	private final Instant committerDate;
	private final String message;

	public Commit(RepoId repoId, CommitHash hash, boolean reachable, boolean tracked, String author,
		Instant authorDate, String committer, Instant committerDate, String message) {

		this.repoId = repoId;
		this.hash = hash;
		this.reachable = reachable;
		this.tracked = tracked;
		this.author = author;
		this.authorDate = authorDate;
		this.committer = committer;
		this.committerDate = committerDate;
		this.message = message;
	}

	/**
	 * Returns a placeholder commit to be used in situations where you absolutely need a {@link
	 * Commit} but don't have any available.
	 *
	 * @param repoId the commit's repo id
	 * @param hash the commit's hash
	 * @return a placeholder commit with the specified repo id and hash and placeholder values in all
	 * 	other fields.
	 */
	public static Commit placeholder(RepoId repoId, CommitHash hash) {
		return new Commit(
			repoId,
			hash,
			false,
			false,
			"N/A",
			Instant.EPOCH,
			"N/A",
			Instant.EPOCH,
			"N/A"
		);
	}

	public RepoId getRepoId() {
		return repoId;
	}

	public CommitHash getHash() {
		return hash;
	}

	public String getHashAsString() {
		return hash.getHash();
	}

	/**
	 * @return whether this commit can be reached from any branch.
	 */
	public boolean isReachable() {
		return reachable;
	}

	/**
	 * A commit is tracked if it is reachable from any tracked branch.
	 *
	 * @return whether this commit is tracked
	 */
	public boolean isTracked() {
		return tracked;
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

	public Pair<String, Optional<String>> getSections() {
		String[] split = message.split("\n\n", 2);
		if (split.length == 2) {
			return new Pair<>(split[0] + "\n", Optional.of(split[1]));
		} else {
			return new Pair<>(message, Optional.empty());
		}
	}

	public String getSummary() {
		return getSections().getFirst();
	}

	public Optional<String> getMessageWithoutSummary() {
		return getSections().getSecond();
	}
}
