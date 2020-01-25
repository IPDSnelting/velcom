package de.aaaaaaah.velcom.backend.access.repocomparison;

import de.aaaaaaah.velcom.backend.access.benchmark.Interpretation;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.benchmark.Unit;
import java.util.ArrayList;
import java.util.List;

public class TmpMeasurement {

	private final String measurementId;
	private final MeasurementName measurementName;
	private final Interpretation interpretation;
	private final Unit unit;
	private final List<Double> values;

	TmpMeasurement(String measurementId, MeasurementName measurementName,
		Interpretation interpretation, Unit unit) {

		this.measurementId = measurementId;
		this.measurementName = measurementName;
		this.interpretation = interpretation;
		this.unit = unit;

		values = new ArrayList<>();
	}

	public String getMeasurementId() {
		return measurementId;
	}

	public MeasurementName getMeasurementName() {
		return measurementName;
	}

	public Interpretation getInterpretation() {
		return interpretation;
	}

	public Unit getUnit() {
		return unit;
	}

	public List<Double> getValues() {
		return values;
	}

	public double getValue() {
		return MeasurementValues.getValue(values);
	}

	@Override
	public String toString() {
		return "TmpMeasurement{" +
			"measurementId='" + measurementId + '\'' +
			", measurementName=" + measurementName +
			", interpretation=" + interpretation +
			", unit=" + unit +
			", values=" + values +
			'}';
	}
}
