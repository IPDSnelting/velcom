package de.aaaaaaah.velcom.backend.access.token;

import de.aaaaaaah.velcom.backend.access.repo.RepoId;

/**
 * A token for authenticating, either tied to a single {@link RepoId} or working globally.
 *
 * <p> Whether the token is valid for a single repo or globally is not stored in the token itself.
 * The {@link AuthToken} class is only meant for passing a token to the {@link TokenAccess}, where
 * it can then be evaluated and checked.
 *
 * <p> To check whether a token is valid for a specific repo, use {@link
 * de.aaaaaaah.velcom.backend.access.repo.Repo#isValidToken(AuthToken)}. To check whether a token is
 * a valid admin token, use {@link TokenAccess#isValidAdminToken(AuthToken)}.
 */
public class AuthToken {

	private final String token;

	public AuthToken(String token) {
		this.token = token;
	}

	public String getToken() {
		return token;
	}

	@Override
	public String toString() {
		return "AuthToken{" +
			"token='" + token + '\'' +
			'}';
	}
}
