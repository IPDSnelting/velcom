package de.aaaaaaah.velcom.backend.newaccess.entities;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a successful {@link Measurement}'s state, which contains the measurement
 * values as well as their {@link Unit} and {@link Interpretation}.
 */
public class MeasurementValues {

	public static double calculateAverage(List<Double> values) {
		double sum = 0;
		for (Double value : values) {
			sum += value;
		}

		return sum / values.size();
	}

	private final List<Double> values;
	private final Unit unit;
	private final Interpretation interpretation;

	public MeasurementValues(List<Double> values, Unit unit, Interpretation interpretation) {
		this.values = Collections.unmodifiableList(Objects.requireNonNull(values));
		this.unit = Objects.requireNonNull(unit);
		this.interpretation = Objects.requireNonNull(interpretation);
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

	public double getAverageValue() {
		return calculateAverage(values);
	}


}
