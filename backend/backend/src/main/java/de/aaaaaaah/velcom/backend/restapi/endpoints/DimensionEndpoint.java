package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionWriteAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.restapi.authentication.Admin;
import io.dropwizard.auth.Auth;
import io.micrometer.core.annotation.Timed;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for deleting dimensions.
 */
@Path("/dimension/{benchmark}/{metric}")
@Produces(MediaType.APPLICATION_JSON)
public class DimensionEndpoint {

	private final DimensionWriteAccess dimensionAccess;

	public DimensionEndpoint(DimensionWriteAccess dimensionAccess) {
		this.dimensionAccess = dimensionAccess;
	}

	@DELETE
	@Timed(histogram = true)
	public void delete(
		@Auth Admin admin,
		@PathParam("benchmark") String benchmark,
		@PathParam("metric") String metric
	) {
		dimensionAccess.deleteDimension(new Dimension(benchmark, metric));
	}
}
