package de.aaaaaaah.velcom.backend.restapi;

import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.access.token.AuthToken;
import de.aaaaaaah.velcom.backend.access.token.TokenAccess;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

public class RepoAuthenticator implements Authenticator<BasicCredentials, RepoUser> {

	private final TokenAccess tokenAccess;

	public RepoAuthenticator(TokenAccess tokenAccess) {
		this.tokenAccess = tokenAccess;
	}

	@Override
	public Optional<RepoUser> authenticate(BasicCredentials basicCredentials)
		throws AuthenticationException {

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
