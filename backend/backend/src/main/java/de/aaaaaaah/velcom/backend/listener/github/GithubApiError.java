package de.aaaaaaah.velcom.backend.listener.github;

import java.net.URI;

public class GithubApiError extends Exception {

	private final URI url;

	public GithubApiError(String message, URI url) {
		super("Error while accessing " + url + ": " + message);
		this.url = url;
	}

	public URI getUrl() {
		return url;
	}
}
