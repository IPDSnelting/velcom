package de.aaaaaaah.designproto.backend.access.token;

import de.aaaaaaah.designproto.backend.access.AccessLayer;
import de.aaaaaaah.designproto.backend.access.commit.CommitAccess;
import de.aaaaaaah.designproto.backend.access.repo.RepoId;
import de.aaaaaaah.designproto.backend.storage.db.DatabaseStorage;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * This class abstracts away access to the {@link AuthToken}s which are stored in the db.
 *
 * <p> TODO add more details on how tokens are not stored in plain text etc.
 */
public class TokenAccess {

	private final AccessLayer accessLayer;
	private final DatabaseStorage databaseStorage;
	private final AuthToken adminToken;

	/**
	 * This constructor also registers the {@link CommitAccess} in the accessLayer.
	 *
	 * @param accessLayer the {@link AccessLayer} to register with
	 * @param databaseStorage a database storage
	 * @param adminToken the admin token from the config
	 */
	public TokenAccess(AccessLayer accessLayer, DatabaseStorage databaseStorage,
		AuthToken adminToken) {

		this.accessLayer = accessLayer;
		this.databaseStorage = databaseStorage;
		this.adminToken = adminToken;

		accessLayer.registerTokenAccess(this);
	}

	// Querying tokens

	/**
	 * @param repoId a repo's id
	 * @param token a token to test
	 * @return whether the token is valid for the specified repo. Defaults to false if the repo does
	 * 	not exist or has no token
	 */
	public boolean isValidToken(RepoId repoId, AuthToken token) {
		// TODO implement
		return false;
	}

	/**
	 * @param token a token to test
	 * @return whether the token should give admin powers
	 */
	public boolean isValidAdminToken(AuthToken token) {
		// TODO implement
		return false;
	}

	// Modifying tokens

	public void setToken(RepoId id, @Nullable AuthToken authToken) {
		// TODO implement
	}

	// Cleanup

	/**
	 * Delete all tokens whose corresponding repo doesn't exist (any more).
	 */
	public void deleteAllUnused() {
		// TODO implement
	}
}
