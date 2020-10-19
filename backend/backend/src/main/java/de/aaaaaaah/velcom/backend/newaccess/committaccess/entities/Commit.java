package de.aaaaaaah.velcom.backend.newaccess.committaccess.entities;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Commit {

	private final RepoId repoId;
	private final CommitHash hash;
	private final boolean tracked;
	private final String author;
	private final Instant authorDate;
	private final String committer;
	private final Instant committerDate;
	private final String message;

	public Commit(RepoId repoId, CommitHash hash, boolean tracked, String author, Instant authorDate,
		String committer, Instant committerDate, String message) {

		this.repoId = repoId;
		this.hash = hash;
		this.tracked = tracked;
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

	public String getHashAsString() {
		return hash.getHash();
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

	public List<String> getSections() {
		List<String> lines = message.lines().collect(Collectors.toList());

		int firstEmptyLine = lines.indexOf("");
		if (firstEmptyLine == -1) {
			// No empty line, meaning there is only a summary and no other sections
			return List.of(message);
		}

		String summary = lines.subList(0, firstEmptyLine).stream()
			.collect(Collectors.joining("\n", "", "\n"));
		String rest = lines.subList(firstEmptyLine + 1, lines.size()).stream()
			.collect(Collectors.joining("\n", "", "\n"));
		return List.of(summary, rest);
	}

	public String getSummary() {
		return getSections().get(0);
	}

	public Optional<String> getMessageWithoutSummary() {
		List<String> sections = getSections();
		if (sections.size() == 2) {
			return Optional.of(sections.get(1));
		} else {
			return Optional.empty();
		}
	}
}
