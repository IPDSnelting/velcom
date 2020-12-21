package de.aaaaaaah.velcom.backend.restapi.authentication;

import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.access.tokenaccess.TokenReadAccess;
import de.aaaaaaah.velcom.backend.access.tokenaccess.entities.AuthToken;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * An authenticator for admin and repo admin access.
 */
public class RepoAuthenticator implements Authenticator<BasicCredentials, RepoUser> {

	private final TokenReadAccess tokenAccess;

	public RepoAuthenticator(TokenReadAccess tokenAccess) {
		this.tokenAccess = tokenAccess;
	}

	@Override
	public Optional<RepoUser> authenticate(BasicCredentials basicCredentials) {

		@Nullable RepoId repoId;
		String username = basicCredentials.getUsername();
		if (username.equals("admin")) {
			repoId = null;
		} else {
			repoId = new RepoId(UUID.fromString(username));
		}

		AuthToken authToken = new AuthToken(basicCredentials.getPassword());
		RepoUser repoUser = new RepoUser(repoId);

		if (repoId == null) {
			if (tokenAccess.isValidAdminToken(authToken)) {
				return Optional.of(repoUser);
			}
		} else {
			if (tokenAccess.isValidToken(repoId, authToken)) {
				return Optional.of(repoUser);
			}
		}
		return Optional.empty();
	}
}
