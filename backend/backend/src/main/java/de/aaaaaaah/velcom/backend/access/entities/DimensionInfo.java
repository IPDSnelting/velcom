package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Objects;

public class DimensionInfo {

	private final Dimension name;
	private final Unit unit;
	private final Interpretation interpretation;

	public DimensionInfo(Dimension name, Unit unit, Interpretation interpretation) {
		this.name = Objects.requireNonNull(name);
		this.unit = Objects.requireNonNull(unit);
		this.interpretation = Objects.requireNonNull(interpretation);
	}

	public static DimensionInfo fromMeasurement(Measurement measurement) {
		return new DimensionInfo(
			measurement.getMeasurementName(),
			measurement.getUnit(),
			measurement.getInterpretation()
		);
	}

	public Dimension getName() {
		return name;
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
		return name.equals(dimensionInfo.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return "Dimension{" +
			"name=" + name +
			", unit=" + unit +
			", interpretation=" + interpretation +
			'}';
	}

}
