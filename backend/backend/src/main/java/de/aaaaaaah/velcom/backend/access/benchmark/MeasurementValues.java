package de.aaaaaaah.velcom.backend.access.benchmark;

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

	public static double getValue(List<Double> values) {
		double sum = 0;
		for (Double value : values) {
			sum += value;
		}

		return sum / values.size();
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

	public double getValue() {
		return getValue(values);
	}
}
