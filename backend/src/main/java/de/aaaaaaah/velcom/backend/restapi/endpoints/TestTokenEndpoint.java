package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.access.token.TokenAccess;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 * The REST API endpoint allowing to test access tokens.
 */
@Path("/test-token")
public class TestTokenEndpoint {

	private final TokenAccess tokenAccess;

	public TestTokenEndpoint(TokenAccess tokenAccess) {
		this.tokenAccess = tokenAccess;
	}

	/**
	 * Returns the push notification token that can be used to send push requests.
	 *
	 * @param repoUuid the id of the repo
	 */
	@GET
	public void get(@NotNull @QueryParam("repo_id") UUID repoUuid) {
		RepoId repoId = new RepoId(repoUuid);
	}

}
