package de.aaaaaaah.velcom.backend.data.runcomparison;

import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import java.util.Optional;
import javax.annotation.Nullable;

public class DimensionDifference {

	private final Dimension dimension;
	private final double first;
	private final double second;
	private final RunId oldRunId;
	@Nullable
	private final Double secondStddev;

	public DimensionDifference(Dimension dimension, double first, double second,
		RunId oldRunId, @Nullable Double secondStddev) {

		this.dimension = dimension;
		this.first = first;
		this.second = second;
		this.oldRunId = oldRunId;
		this.secondStddev = secondStddev;
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

	public RunId getOldRunId() {
		return oldRunId;
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
}
