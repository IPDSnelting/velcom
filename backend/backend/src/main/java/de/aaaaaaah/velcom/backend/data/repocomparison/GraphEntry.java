package de.aaaaaaah.velcom.backend.data.repocomparison;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.Commit;
import java.time.Instant;
import java.util.Objects;

public class GraphEntry {

	private final CommitHash hash;
	private final String author;
	private final Instant authorDate;
	private final String summary;
	private final double value;

	public GraphEntry(Commit commit, double value) {
		Objects.requireNonNull(commit);

		this.hash = commit.getHash();
		this.author = commit.getAuthor();
		this.authorDate = commit.getAuthorDate();
		this.summary = commit.getSummary();
		this.value = value;
	}

	public CommitHash getHash() {
		return hash;
	}

	public String getAuthor() {
		return author;
	}

	public Instant getAuthorDate() {
		return authorDate;
	}

	public String getSummary() {
		return summary;
	}

	public double getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "GraphEntry{" +
			"hash=" + hash +
			", value=" + value +
			'}';
	}

}
