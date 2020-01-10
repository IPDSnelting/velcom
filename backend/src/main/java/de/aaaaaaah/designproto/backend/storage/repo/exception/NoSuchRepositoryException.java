package de.aaaaaaah.designproto.backend.storage.repo.exception;

import de.aaaaaaah.designproto.backend.storage.repo.RepoStorage;
import java.util.NoSuchElementException;

/**
 * An exception that occurs when trying to access a repository stored in a {@link RepoStorage} which
 * does not exist.
 */
public class NoSuchRepositoryException extends NoSuchElementException {

	private final String dirName;
	private final RepoStorage storage;

	/**
	 * Consturcts a new {@link NoSuchRepositoryException}.
	 *
	 * @param dirName the directory that is missing a repository
	 * @param storage the storage
	 */
	public NoSuchRepositoryException(String dirName, RepoStorage storage) {
		super("no repository exists with given name " + dirName + " in storage " + storage);
		this.dirName = dirName;
		this.storage = storage;
	}

	/**
	 * @return the directory that is missing the repository
	 */
	public String getDirName() {
		return dirName;
	}

	/**
	 * @return the storage where the repository is missing in
	 */
	public RepoStorage getStorage() {
		return storage;
	}

}
