package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import de.aaaaaaah.velcom.backend.data.runcomparison.RunComparison;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Dimension;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.DimensionInfo;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class JsonDimensionDifference {

	private final JsonDimension dimension;
	private final UUID oldRunId;
	private final double diff;
	@Nullable
	private final Double reldiff;
	@Nullable
	private final Double stddevDiff;

	public JsonDimensionDifference(JsonDimension dimension, UUID oldRunId, double diff,
		@Nullable Double reldiff, @Nullable Double stddevDiff) {

		this.dimension = dimension;
		this.oldRunId = oldRunId;
		this.diff = diff;
		this.reldiff = reldiff;
		this.stddevDiff = stddevDiff;
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
			difference.getOldRunId().getId(),
			difference.getDiff(),
			difference.getReldiff().orElse(null),
			difference.getStddevDiff().orElse(null)
		);
	}

	public static List<JsonDimensionDifference> fromRunComparison(RunComparison comparison,
		Map<Dimension, DimensionInfo> dimensionInfos) {

		return comparison.getDifferences().stream()
			.map(diff -> fromDimensionDifference(diff, dimensionInfos))
			.collect(Collectors.toList());
	}

	public JsonDimension getDimension() {
		return dimension;
	}

	public UUID getOldRunId() {
		return oldRunId;
	}

	public double getDiff() {
		return diff;
	}

	@Nullable
	public Double getReldiff() {
		return reldiff;
	}

	@Nullable
	public Double getStddevDiff() {
		return stddevDiff;
	}
}
