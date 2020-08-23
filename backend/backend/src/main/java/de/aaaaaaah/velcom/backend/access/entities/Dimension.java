package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Objects;

public class Dimension {

	private final MeasurementName name;
	private final Unit unit;
	private final Interpretation interpretation;

	public Dimension(MeasurementName name, Unit unit, Interpretation interpretation) {
		this.name = Objects.requireNonNull(name);
		this.unit = Objects.requireNonNull(unit);
		this.interpretation = Objects.requireNonNull(interpretation);
	}

	public static Dimension fromMeasurement(Measurement measurement) {
		return new Dimension(
			measurement.getMeasurementName(),
			measurement.getUnit(),
			measurement.getInterpretation()
		);
	}

	public MeasurementName getName() {
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
		Dimension dimension = (Dimension) o;
		return name.equals(dimension.name);
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
