package de.aaaaaaah.velcom.backend.newaccess.dimensionaccess.entities;

import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import java.util.Objects;

public class DimensionInfo {

	private final Dimension dimension;
	private final Unit unit;
	private final Interpretation interpretation;

	public DimensionInfo(Dimension dimension, Unit unit, Interpretation interpretation) {
		this.dimension = Objects.requireNonNull(dimension);
		this.unit = Objects.requireNonNull(unit);
		this.interpretation = Objects.requireNonNull(interpretation);
	}

	/**
	 * Create a {@link DimensionInfo} with default unit and interpretation.
	 *
	 * @param dimension the dimension
	 */
	public DimensionInfo(Dimension dimension) {
		this(dimension, Unit.DEFAULT, Interpretation.DEFAULT);
	}

	public static DimensionInfo fromMeasurement(Measurement measurement, Unit unit,
		Interpretation interpretation) {

		return new DimensionInfo(
			measurement.getDimension(),
			unit,
			interpretation
		);
	}

	public Dimension getDimension() {
		return dimension;
	}

	public Unit getUnit() {
		return unit;
	}

	public Interpretation getInterpretation() {
		return interpretation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DimensionInfo dimensionInfo = (DimensionInfo) o;
		return dimension.equals(dimensionInfo.dimension);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dimension);
	}

	@Override
	public String toString() {
		return "DimensionInfo{" +
			"dimension=" + dimension +
			", unit=" + unit +
			", interpretation=" + interpretation +
			'}';
	}
}
