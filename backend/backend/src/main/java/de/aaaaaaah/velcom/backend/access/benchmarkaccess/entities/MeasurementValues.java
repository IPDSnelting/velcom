package de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities;

import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.dimensionaccess.entities.Unit;
import de.aaaaaaah.velcom.backend.data.significance.SignificanceFactors;
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

	/**
	 * @param significanceFactors the significance factors to use
	 * @return the standard deviation of the values, if there are more than {@link
	 *  SignificanceFactors#getMinStddevAmount()} values
	 */
	public Optional<Double> getStddevWith(SignificanceFactors significanceFactors) {
		if (values.size() >= significanceFactors.getMinStddevAmount()) {
			return getStddev();
		} else {
			return Optional.empty();
		}
	}

	/**
	 * @return the standard deviation of the values, if there are at least two values. WARNING: When
	 * 	calling this function, always use {@link SignificanceFactors#getMinStddevAmount()} to see
	 * 	whether a standard deviation even makes sense in this situation.
	 */
	private Optional<Double> getStddev() {
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
	 * @param significanceFactors the significance factors to use
	 * @return stddev / averageValue, if there are more than {@link SignificanceFactors#getMinStddevAmount()}
	 * 	values
	 */
	public Optional<Double> getStddevPercentWith(SignificanceFactors significanceFactors) {
		return getStddevWith(significanceFactors)
			.map(it -> it / getAverageValue());
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
