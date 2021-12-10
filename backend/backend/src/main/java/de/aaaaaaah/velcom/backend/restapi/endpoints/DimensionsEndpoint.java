package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionWriteAccess;
import de.aaaaaaah.velcom.backend.restapi.authentication.Admin;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimensionId;
import io.dropwizard.auth.Auth;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for deleting dimensions.
 */
@Path("/dimensions/")
@Produces(MediaType.APPLICATION_JSON)
public class DimensionsEndpoint {

	private final DimensionWriteAccess dimensionAccess;

	public DimensionsEndpoint(DimensionWriteAccess dimensionAccess) {
		this.dimensionAccess = dimensionAccess;
	}

	@DELETE
	@Timed(histogram = true)
	public void delete(
		@Auth Admin admin,
		@NotNull List<JsonDimensionId> request
	) {
		dimensionAccess.deleteDimensions(
			request
				.stream()
				.map(JsonDimensionId::toDimension)
				.collect(Collectors.toList())
		);
	}
}
