package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import de.aaaaaaah.velcom.backend.access.entities.Interpretation;

public class JsonDimension {

	private final String benchmark;
	private final String metric;
	private final String unit;
	private final String interpretation;

	public JsonDimension(String benchmark, String metric, String unit,
		Interpretation interpretation) {

		this.benchmark = benchmark;
		this.metric = metric;
		this.unit = unit;
		this.interpretation = interpretation.getTextualRepresentation();
	}

	/**
	 * Create a {@link JsonDimension} from a {@link DimensionInfo}.
	 *
	 * @param dimensionInfo the {@link DimensionInfo} to use
	 * @return the newly created {@link JsonDimension}
	 */
	public static JsonDimension fromDimensionInfo(DimensionInfo dimensionInfo) {
		return new JsonDimension(
			dimensionInfo.getDimension().getBenchmark(),
			dimensionInfo.getDimension().getMetric(),
			dimensionInfo.getUnit().getName(),
			dimensionInfo.getInterpretation()
		);
	}

	public String getBenchmark() {
		return benchmark;
	}

	public String getMetric() {
		return metric;
	}

	public String getUnit() {
		return unit;
	}

	public String getInterpretation() {
		return interpretation;
	}
}
