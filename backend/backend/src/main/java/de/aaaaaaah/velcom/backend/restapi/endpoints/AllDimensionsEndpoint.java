package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import io.micrometer.core.annotation.Timed;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Endpoint for retrieving a list of all dimensions.
 */
@Path("/all-dimensions")
@Produces(MediaType.APPLICATION_JSON)
public class AllDimensionsEndpoint {

	private final DimensionReadAccess dimensionAccess;

	public AllDimensionsEndpoint(DimensionReadAccess dimensionAccess) {
		this.dimensionAccess = dimensionAccess;
	}

	@GET
	@Timed(histogram = true)
	public GetReply get() {
		List<JsonDimension> dimensions = dimensionAccess.getAllDimensions()
			.stream()
			.sorted(Comparator.comparing(DimensionInfo::getDimension))
			.map(JsonDimension::fromDimensionInfo)
			.collect(Collectors.toList());

		return new GetReply(dimensions);
	}

	private static class GetReply {

		public final List<JsonDimension> dimensions;

		public GetReply(List<JsonDimension> dimensions) {
			this.dimensions = dimensions;
		}
	}
}
