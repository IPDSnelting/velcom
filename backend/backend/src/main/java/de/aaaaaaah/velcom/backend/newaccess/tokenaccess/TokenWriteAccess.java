package de.aaaaaaah.velcom.backend.newaccess.tokenaccess;

import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.tokenaccess.entities.AuthToken;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Access for setting and removing auth tokens on repos.
 */
public class TokenWriteAccess extends TokenReadAccess {

	public TokenWriteAccess(DatabaseStorage databaseStorage, AuthToken adminToken, int hashIterations,
		int hashMemory, int hashParallelism) {

		super(databaseStorage, adminToken, hashIterations, hashMemory, hashParallelism);
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
