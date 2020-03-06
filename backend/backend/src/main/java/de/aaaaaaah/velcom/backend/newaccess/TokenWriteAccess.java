package de.aaaaaaah.velcom.backend.newaccess;

import de.aaaaaaah.velcom.backend.newaccess.entities.AuthToken;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.Objects;
import javax.annotation.Nullable;

public class TokenWriteAccess extends TokenReadAccess {

	public TokenWriteAccess(DatabaseStorage databaseStorage, AuthToken adminToken, int hashMemory,
		int hashIterations) {

		super(databaseStorage, adminToken, hashMemory, hashIterations);
	}

	/**
	 * Sets (or removes) the repo token of the specified repository.
	 *
	 * @param id the id of the repository
	 * @param authToken the token to set, or {@code null} if the existing token is to be removed.
	 */
	public void setToken(RepoId id, @Nullable AuthToken authToken) {
		Objects.requireNonNull(id);
		setTokenProtected(id, authToken);
	}

}
