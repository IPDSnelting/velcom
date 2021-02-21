package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.restapi.authentication.Admin;
import io.dropwizard.auth.Auth;
import io.micrometer.core.annotation.Timed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
	 * @param admin auth guard
	 */
	@GET
	@Timed(histogram = true)
	public void get(@Auth Admin admin) {
		// Being useful by doing nothing, when does that ever happen...
	}

}
