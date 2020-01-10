package de.aaaaaaah.designproto.backend.access.benchmark;

import java.util.List;

/**
 * This class represents a successful {@link Measurement}'s state, which contains the measurement
 * values as well as their {@link Unit} and {@link Interpretation}.
 */
public class MeasurementValues {

	private final List<Double> values;
	private final Unit unit;
	private final Interpretation interpretation;

	MeasurementValues(List<Double> values, Unit unit, Interpretation interpretation) {
		this.values = List.copyOf(values); // immutable
		this.unit = unit;
		this.interpretation = interpretation;
	}

	public List<Double> getValues() {
		return values;
	}

	public Unit getUnit() {
		return unit;
	}

	public Interpretation getInterpretation() {
		return interpretation;
	}

	// TODO decide on which algorithm to use for combining the values. A simple average?

	/**
	 * Combines all the measured values into a single value.
	 *
	 * @return the combined value
	 */
	public double getValue() {
		// TODO implement
		return 0;
	}
}
