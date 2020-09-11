package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.restapi.authentication.RepoUser;
import io.dropwizard.auth.Auth;
import java.util.UUID;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 * Endpoint for testing validity of access tokens.
 */
@Path("/test-token")
public class TestTokenEndpoint {

	public TestTokenEndpoint() {
	}

	/**
	 * Checks whether an access token is valid for a given repo.
	 *
	 * @param user the user trying to authenticate
	 * @param repoUuid the id of the repo
	 */
	@GET
	public void get(@Auth RepoUser user, @QueryParam("repo_id") UUID repoUuid) {
		if (repoUuid == null) {
			user.guardAdminAccess();
		} else {
			RepoId repoId = new RepoId(repoUuid);
			user.guardRepoAccess(repoId);
		}
	}

}
