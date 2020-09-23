package de.aaaaaaah.velcom.backend.access.exceptions;

/**
 * An exception thrown when a git repo doesn't fulfill the required properties.
 */
public class MalformedRepoException extends Exception {

	public MalformedRepoException(String message) {
		super(message);
	}
}
