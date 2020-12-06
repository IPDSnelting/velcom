package de.aaaaaaah.velcom.backend.access.entities;

import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities.Unit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
		if (this.values.isEmpty()) {
			throw new IllegalArgumentException("list of values must not be empty");
		}
	}

	public List<Double> getValues() {
		return values;
	}

	public double getAverageValue() {
		return calculateAverage(values);
	}

	public Optional<Double> getStddev() {
		int n = values.size();
		if (n < 2) {
			return Optional.empty();
		}

		double avg = getAverageValue();
		double sum = values.stream()
			.map(value -> (value - avg) * (value - avg))
			.reduce(0.0, Double::sum);
		double result = Math.sqrt(sum / (n - 1));
		return Optional.of(result);
	}

	/**
	 * @return stddev / averageValue
	 */
	public Optional<Double> getStddevPercent() {
		if (getAverageValue() == 0) {
			return Optional.empty();
		}
		return getStddev().map(it -> it / getAverageValue());
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
