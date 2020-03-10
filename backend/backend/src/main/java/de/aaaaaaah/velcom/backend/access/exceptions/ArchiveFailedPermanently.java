package de.aaaaaaah.velcom.backend.access.exceptions;

import de.aaaaaaah.velcom.backend.access.archive.ArchiveException;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;

/**
 * Archiving failed and retrying is probably not going to help.
 */
public class ArchiveFailedPermanently extends ArchiveException {

	public ArchiveFailedPermanently(Throwable cause, String dirName, CommitHash commitHash) {
		super(cause, dirName, commitHash);
	}
}
