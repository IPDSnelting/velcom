package de.aaaaaaah.velcom.backend;

import java.time.Instant;
import java.util.Optional;
import javax.annotation.Nullable;

public class TestCommit {

	private static String dummyContent() {
		return String.valueOf(System.currentTimeMillis());
	}

	private final String message;
	private final String file;
	private final String content;
	private final @Nullable
	String branch;
	private final @Nullable
	Instant authorDate;

	public TestCommit(String message) {
		this(message, "afile.txt", dummyContent(), null, null);
	}

	public TestCommit(String message, Instant authorDate) {
		this(message, "afile.txt", dummyContent(), null, authorDate);
	}

	public TestCommit(String message, String branch) {
		this(message, "afile.txt", dummyContent(), branch, null);
	}

	public TestCommit(String message, String branch, Instant authorDate) {
		this(message, "afile.txt", dummyContent(), branch, authorDate);
	}

	public TestCommit(String message, String file, String content) {
		this(message, file, content, null, null);
	}

	public TestCommit(String message, String file, String content,
		@Nullable String branch, @Nullable Instant authorDate) {

		this.message = message;
		this.file = file;
		this.content = content;
		this.branch = branch;
		this.authorDate = authorDate;
	}

	public String getMessage() {
		return message;
	}

	public String getFile() {
		return file;
	}

	public String getContent() {
		return content;
	}

	public Optional<String> getBranch() {
		return Optional.ofNullable(branch);
	}

	public Optional<Instant> getAuthorDate() {
		return Optional.ofNullable(authorDate);
	}

	@Override
	public String toString() {
		return "TestCommit{" +
			"message='" + message + '\'' +
			", file='" + file + '\'' +
			", content='" + content + '\'' +
			", branch='" + branch + '\'' +
			", authorDate=" + authorDate +
			'}';
	}

}
