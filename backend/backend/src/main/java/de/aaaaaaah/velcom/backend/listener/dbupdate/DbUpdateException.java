package de.aaaaaaah.velcom.backend.listener.dbupdate;

/**
 * An exception thrown when the {@link DbUpdater} failed to update the database for a repo.
 */
public class DbUpdateException extends Exception {

	public DbUpdateException(String message, Throwable cause) {
		super(message, cause);
	}
}
