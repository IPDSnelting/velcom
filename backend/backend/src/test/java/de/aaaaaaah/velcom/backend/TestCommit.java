package de.aaaaaaah.velcom.backend;

public class TestCommit {

	private final String message;
	private final String file;
	private final String content;

	public TestCommit(String message, String file, String content) {
		this.message = message;
		this.file = file;
		this.content = content;
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

	@Override
	public String toString() {
		return "TestCommit{" +
			"message='" + message + '\'' +
			", file='" + file + '\'' +
			", content='" + content + '\'' +
			'}';
	}

}
