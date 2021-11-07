package de.aaaaaaah.velcom.backend.restapi.endpoints;

import de.aaaaaaah.velcom.backend.access.dimensionaccess.DimensionReadAccess;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonDimension;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
		Set<DimensionInfo> dimensions = dimensionAccess.getAllDimensions();
		Map<Dimension, Integer> runs = dimensionAccess.getRunsPerDimension();

		List<DimensionEntry> entries = dimensions.stream()
			.map(info -> new DimensionEntry(
				JsonDimension.fromDimensionInfo(info),
				info.isSignificant(),
				runs.getOrDefault(info.getDimension(), 0)
			))
			.collect(Collectors.toList());

		return new GetReply(entries);
	}

	private static class DimensionEntry {

		public final JsonDimension dimension;
		public final boolean significant;
		public final int runs;

		public DimensionEntry(JsonDimension dimension, boolean significant, int runs) {
			this.dimension = dimension;
			this.significant = significant;
			this.runs = runs;
		}
	}

	private static class GetReply {

		public final List<DimensionEntry> dimensions;

		public GetReply(List<DimensionEntry> dimensions) {
			this.dimensions = dimensions;
		}
	}
}
