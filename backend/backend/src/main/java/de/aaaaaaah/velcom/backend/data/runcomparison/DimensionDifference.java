package de.aaaaaaah.velcom.backend.data.runcomparison;

import de.aaaaaaah.velcom.backend.access.entities.DimensionInfo;
import java.util.Optional;
import javax.annotation.Nullable;

public class DimensionDifference {

	private final SignificanceFactors significanceFactors;
	private final DimensionInfo dimensionInfo;
	private final double first;
	private final double second;
	@Nullable
	private final Double secondStddev;

	public DimensionDifference(SignificanceFactors significanceFactors, DimensionInfo dimensionInfo,
		double first, double second, @Nullable Double secondStddev) {

		this.significanceFactors = significanceFactors;
		this.dimensionInfo = dimensionInfo;
		this.first = first;
		this.second = second;
		this.secondStddev = secondStddev;
	}

	public SignificanceFactors getSignificanceFactors() {
		return significanceFactors;
	}

	public DimensionInfo getDimension() {
		return dimensionInfo;
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

	public double getAbsdiff() {
		return second - first;
	}

	public Optional<Double> getReldiff() {
		if (first == 0) {
			return Optional.empty();
		}

		return Optional.of(second / first);
	}

	public boolean isSignificant() {
		boolean relSignificant = getReldiff()
			.map(reldiff -> Math.abs(reldiff) >= significanceFactors.getRelativeFactor())
			.orElse(false);

		boolean stddevSignificant = getSecondStddev()
			.map(stddev -> Math.abs(getAbsdiff()) >= significanceFactors.getStddevFactor() * stddev)
			.orElse(true);

		return relSignificant && stddevSignificant;
	}
}
