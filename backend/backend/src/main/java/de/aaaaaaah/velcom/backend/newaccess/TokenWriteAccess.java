package de.aaaaaaah.velcom.backend.newaccess;

import de.aaaaaaah.velcom.backend.newaccess.entities.AuthToken;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import javax.annotation.Nullable;

public class TokenWriteAccess extends TokenReadAccess {

	public TokenWriteAccess(DatabaseStorage databaseStorage, AuthToken adminToken, int hashMemory,
		int hashIterations) {

		super(databaseStorage, adminToken, hashMemory, hashIterations);
	}

	public void setToken(RepoId id, @Nullable AuthToken authToken) {
		setTokenProtected(id, authToken);
	}
}
