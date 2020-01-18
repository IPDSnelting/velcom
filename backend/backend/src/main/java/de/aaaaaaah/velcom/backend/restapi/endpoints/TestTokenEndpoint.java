package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.restapi.RepoUser;
import io.dropwizard.auth.Auth;
import java.util.UUID;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 * The REST API endpoint allowing to test access tokens.
 */
@Path("/test-token")
public class TestTokenEndpoint {

	public TestTokenEndpoint() {
	}

	/**
	 * Checks whether an access token is valid for a given repo.
	 *
	 * @param repoUuid the id of the repo
	 */
	@POST
	public void post(@Auth RepoUser user, @QueryParam("repo_id") UUID repoUuid) {
		if (repoUuid == null) {
			user.guardAdminAccess();
		} else {
			RepoId repoId = new RepoId(repoUuid);
			user.guardRepoAccess(repoId);
		}
	}

}
