package de.aaaaaaah.velcom.backend.access.repo.archive;

import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import java.io.IOException;

/**
 * This exception is thrown when something goes wrong while trying to archive a repository.
 */
public class ArchiveException extends IOException {

	private final String dirName;
	private final CommitHash commitHash;

	/**
	 * Constructs a new archive exception.
	 *
	 * @param cause the cause for the exception
	 * @param dirName the directory name of the repository that should have been archived
	 * @param commitHash the state of the repository at which the archive should've been created
	 */
	public ArchiveException(Throwable cause, String dirName, CommitHash commitHash) {
		super(cause);
		this.dirName = dirName;
		this.commitHash = commitHash;
	}

	/**
	 * @return the directory name of the repository that should have been archived
	 */
	public String getDirName() {
		return dirName;
	}

	/**
	 * @return the state of the repository at which the archive should've been created
	 */
	public CommitHash getCommitHash() {
		return commitHash;
	}

}
