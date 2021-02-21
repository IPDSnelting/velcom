package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.listener.Listener;
import de.aaaaaaah.velcom.backend.restapi.authentication.Admin;
import io.dropwizard.auth.Auth;
import io.micrometer.core.annotation.Timed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/listener")
@Produces(MediaType.APPLICATION_JSON)
public class ListenerEndpoint {

	private final Listener listener;

	public ListenerEndpoint(Listener listener) {
		this.listener = listener;
	}

	@POST
	@Path("/fetch-all")
	@Timed(histogram = true)
	public void post(@Auth Admin admin) {
		listener.updateAllRepos();
	}
}
