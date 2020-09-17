package de.aaaaaaah.velcom.backend.data.runcomparison;

import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import java.util.Optional;
import javax.annotation.Nullable;

public class DimensionDifference {

	private final SignificanceFactors significanceFactors;
	private final Dimension dimension;
	private final double first;
	private final double second;
	@Nullable
	private final Double secondStddev;

	public DimensionDifference(SignificanceFactors significanceFactors, Dimension dimension,
		double first, double second, @Nullable Double secondStddev) {

		this.significanceFactors = significanceFactors;
		this.dimension = dimension;
		this.first = first;
		this.second = second;
		this.secondStddev = secondStddev;
	}

	public SignificanceFactors getSignificanceFactors() {
		return significanceFactors;
	}

	public Dimension getDimension() {
		return dimension;
	}

	public double getFirst() {
		return first;
	}

	public double getSecond() {
		return second;
	}

	public Optional<Double> getSecondStddev() {
		return Optional.ofNullable(secondStddev);
	}

	public double getDiff() {
		return second - first;
	}

	public Optional<Double> getReldiff() {
		if (first == 0) {
			return Optional.empty();
		}

		return Optional.of((second - first) / first);
	}

	public boolean isSignificant() {
		boolean relSignificant = getReldiff()
			.map(reldiff -> Math.abs(reldiff) >= significanceFactors.getRelativeThreshold())
			.orElse(true);

		boolean stddevSignificant = getSecondStddev()
			.map(stddev -> Math.abs(getDiff()) >= significanceFactors.getStddevThreshold() * stddev)
			.orElse(true);

		return relSignificant && stddevSignificant;
	}
}
