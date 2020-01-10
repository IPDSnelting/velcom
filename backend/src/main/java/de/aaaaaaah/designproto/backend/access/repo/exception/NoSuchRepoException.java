package de.aaaaaaah.designproto.backend.access.repo.exception;

import de.aaaaaaah.designproto.backend.access.repo.RepoId;
import java.util.NoSuchElementException;

/**
 * This exception is thrown whenever an invalid {@link RepoId} is used.
 */
public class NoSuchRepoException extends NoSuchElementException {

	private final RepoId invalidId;

	public NoSuchRepoException(RepoId invalidId) {
		this.invalidId = invalidId;
	}

	public RepoId getInvalidId() {
		return invalidId;
	}

	@Override
	public String getMessage() {
		return "no repo with id " + invalidId;
	}
}
