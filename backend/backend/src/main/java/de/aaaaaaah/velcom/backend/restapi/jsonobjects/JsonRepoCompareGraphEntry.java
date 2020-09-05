package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

public class JsonRepoCompareGraphEntry {

	private final String hash;
	private final String author;
	private final long authorDate;
	private final String summary;
	private final double value;

	public JsonRepoCompareGraphEntry(String hash, String author, long authorDate,
		String summary, double value) {
		this.hash = hash;
		this.author = author;
		this.authorDate = authorDate;
		this.summary = summary;
		this.value = value;
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

	public double getValue() {
		return value;
	}


}
