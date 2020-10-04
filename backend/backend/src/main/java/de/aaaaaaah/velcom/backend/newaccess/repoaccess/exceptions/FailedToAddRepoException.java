package de.aaaaaaah.velcom.backend.newaccess.repoaccess.exceptions;

import de.aaaaaaah.velcom.backend.access.entities.RemoteUrl;
import de.aaaaaaah.velcom.shared.util.StringHelper;

// TODO: 04.10.20 Update the db schema to make repo names unique and update javadoc
// Add this text to the javadoc below once the db schema has been updated:
// This is most likely due to another repo with the same name already existing.

/**
 * This exception is thrown when a repo could not be added into the db.
 */
public class FailedToAddRepoException extends Exception {

	private final String name;
	private final RemoteUrl remoteUrl;

	public FailedToAddRepoException(Throwable t, String name, RemoteUrl remoteUrl) {
		super(
			"failed to add repo named " + StringHelper.escape(name)
				+ " for remote url " + StringHelper.escape(remoteUrl.getUrl()),
			t
		);

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
