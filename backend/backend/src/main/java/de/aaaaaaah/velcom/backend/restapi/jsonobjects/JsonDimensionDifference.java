package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import java.util.Map;
import javax.annotation.Nullable;

public class JsonDimensionDifference {

	private final JsonDimension dimension;
	private final double absdiff;
	@Nullable
	private final Double reldiff;
	@Nullable
	private final Double stddev;

	public JsonDimensionDifference(JsonDimension dimension, double absdiff, @Nullable Double reldiff,
		@Nullable Double stddev) {

		this.dimension = dimension;
		this.absdiff = absdiff;
		this.reldiff = reldiff;
		this.stddev = stddev;
	}

	/**
	 * Create a {@link JsonDimensionDifference} from a {@link DimensionDifference}.
	 *
	 * @param difference the {@link DimensionDifference} to use
	 * @return the newly created {@link JsonDimensionDifference}
	 */
	public static JsonDimensionDifference fromDimensionDifference(DimensionDifference difference,
		Map<Dimension, DimensionInfo> dimensionInfos) {

		return new JsonDimensionDifference(
			JsonDimension.fromDimensionInfo(dimensionInfos.get(difference.getDimension())),
			difference.getAbsdiff(),
			difference.getReldiff().orElse(null),
			difference.getSecondStddev().orElse(null)
		);
	}

	public JsonDimension getDimension() {
		return dimension;
	}

	public double getAbsdiff() {
		return absdiff;
	}

	@Nullable
	public Double getReldiff() {
		return reldiff;
	}

	@Nullable
	public Double getStddev() {
		return stddev;
	}
}
