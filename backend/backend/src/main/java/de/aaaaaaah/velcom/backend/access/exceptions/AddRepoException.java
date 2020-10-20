package de.aaaaaaah.velcom.backend.access.exceptions;

import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RemoteUrl;

/**
 * Throws when a repo could not be added.
 */
public class AddRepoException extends RuntimeException {

	private final String name;
	private final RemoteUrl remoteUrl;

	public AddRepoException(String name, RemoteUrl remoteUrl, Throwable cause) {
		super("failed to add repo: " + name + ", " + remoteUrl, cause);
		this.name = name;
		this.remoteUrl = remoteUrl;
	}

	public AddRepoException(String name, RemoteUrl remoteUrl, String message, Throwable cause) {
		super(message, cause);
		this.name = name;
		this.remoteUrl = remoteUrl;
	}

	public String getName() {
		return name;
	}

	public RemoteUrl getRemoteUrl() {
		return remoteUrl;
	}
}
