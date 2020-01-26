package de.aaaaaaah.velcom.backend;

import javax.annotation.Nullable;

public class TestCommit {

	private final String message;
	private final String file;
	private final String content;
	private final String branch;

	public TestCommit(String message) {
		this(message, "afile.txt", String.valueOf(System.currentTimeMillis()));
	}

	public TestCommit(String message, String file, String content) {
		this(message, file, content, null);
	}

	public TestCommit(String message, String file, String content, @Nullable String branch) {
		this.message = message;
		this.file = file;
		this.content = content;
		this.branch = branch;
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

	@Nullable
	public String getBranch() {
		return branch;
	}

	@Override
	public String toString() {
		return "TestCommit{" +
			"message='" + message + '\'' +
			", file='" + file + '\'' +
			", content='" + content + '\'' +
			'}';
	}

}
