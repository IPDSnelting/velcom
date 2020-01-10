package de.aaaaaaah.designproto.backend.storage.repo.exception;

import de.aaaaaaah.designproto.backend.storage.repo.RepoStorage;

/**
 * An exception that occurs when the acquisition of a local repository has failed.
 */
public class RepositoryAcquisitionException extends Exception {

	private final RepoStorage repoStorage;
	private final String dirName;

	/**
	 * Constructs a new {@link RepositoryAcquisitionException}.
	 *
	 * @param repoStorage the storage wherein the acquisition was attempted
	 * @param dirName the directory name of the repository to acquire
	 * @param e the cause for this exception
	 */
	public RepositoryAcquisitionException(RepoStorage repoStorage, String dirName, Exception e) {
		super("Failed to acquire repository " + dirName + " in storage " + repoStorage, e);
		this.repoStorage = repoStorage;
		this.dirName = dirName;
	}

	/**
	 * @return the storage wherein the acquisition was attempted
	 */
	public RepoStorage getRepoStorage() {
		return repoStorage;
	}

	/**
	 * @return the directory name of the repository to acquire
	 */
	public String getDirName() {
		return dirName;
	}

}
