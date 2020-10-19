package de.aaaaaaah.velcom.backend.listener;

public class InvalidRemoteUrlException extends RuntimeException {

	private final String realRemoteUrl;
	private final String targetRemoteUrl;

	public InvalidRemoteUrlException(String realRemoteUrl, String targetRemoteUrl) {
		super("Real remote url " + realRemoteUrl
			+ " does not match target remote url " + targetRemoteUrl);

		this.realRemoteUrl = realRemoteUrl;
		this.targetRemoteUrl = targetRemoteUrl;
	}

	public String getRealRemoteUrl() {
		return realRemoteUrl;
	}

	public String getTargetRemoteUrl() {
		return targetRemoteUrl;
	}
}
