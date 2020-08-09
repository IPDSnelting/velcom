package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a successful {@link Measurement}'s state, which contains the measurement
 * values as well as their {@link Unit} and {@link Interpretation}.
 */
public class MeasurementValues {

	/**
	 * Calculates the mean of the given values.
	 *
	 * @param values the values
	 * @return the mean
	 */
	public static double calculateAverage(List<Double> values) {
		double sum = 0;
		for (Double value : values) {
			sum += value;
		}

		return sum / values.size();
	}

	private final List<Double> values;

	public MeasurementValues(List<Double> values) {
		this.values = Collections.unmodifiableList(Objects.requireNonNull(values));
	}

	public List<Double> getValues() {
		return values;
	}

	public double getAverageValue() {
		return calculateAverage(values);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MeasurementValues that = (MeasurementValues) o;
		return values.equals(that.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(values);
	}

	@Override
	public String toString() {
		return "MeasurementValues{" +
			"values=" + values +
			'}';
	}

}
