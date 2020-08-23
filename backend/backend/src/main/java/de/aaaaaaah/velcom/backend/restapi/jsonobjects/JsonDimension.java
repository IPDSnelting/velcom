package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import de.aaaaaaah.velcom.backend.access.entities.Dimension;
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
	 * Create a {@link JsonDimension} from a {@link Dimension}.
	 *
	 * @param dimension the {@link Dimension} to use
	 * @return the newly created {@link JsonDimension}
	 */
	public static JsonDimension fromDimension(Dimension dimension) {
		return new JsonDimension(
			dimension.getName().getBenchmark(),
			dimension.getName().getMetric(),
			dimension.getUnit().getName(),
			dimension.getInterpretation()
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
