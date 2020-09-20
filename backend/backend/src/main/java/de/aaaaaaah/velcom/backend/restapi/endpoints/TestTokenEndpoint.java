package de.aaaaaaah.velcom.backend.restapi.endpoints;

import static de.aaaaaaah.velcom.backend.util.MetricsUtils.timer;

import com.codahale.metrics.annotation.Timed;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import de.aaaaaaah.velcom.backend.restapi.authentication.RepoUser;
import io.dropwizard.auth.Auth;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 * Endpoint for testing validity of access tokens.
 */
@Path("/test-token")
public class TestTokenEndpoint {

	private static final Timer ENDPOINT_TIMER = timer("velcom.endpoint.testtoken.get");

	public TestTokenEndpoint() {
	}

	/**
	 * Checks whether an access token is valid for a given repo.
	 *
	 * @param user the user trying to authenticate
	 * @param repoUuid the id of the repo
	 */
	@GET
	@Timed
	public void get(@Auth RepoUser user, @QueryParam("repo_id") UUID repoUuid) {
		final var timer = Timer.start();

		if (repoUuid == null) {
			user.guardAdminAccess();
		} else {
			RepoId repoId = new RepoId(repoUuid);
			user.guardRepoAccess(repoId);
		}

		timer.stop(ENDPOINT_TIMER);
	}

}
