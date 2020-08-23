package de.aaaaaaah.velcom.backend.data.runcomparison;

import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.Interpretation;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementName;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.Unit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public class RunComparer {

	private final SignificanceFactors significanceFactors;

	public RunComparer(SignificanceFactors significanceFactors) {
		this.significanceFactors = significanceFactors;
	}

	/**
	 * Compare two {@link Run}s against each other.
	 *
	 * @param first the first run
	 * @param second the second run
	 * @return the differences between the runs
	 */
	public RunComparison compare(Run first, Run second) {
		Map<MeasurementName, MeasurementValues> firstMap = getMeasurementsMap(first);
		Map<MeasurementName, MeasurementValues> secondMap = getMeasurementsMap(second);

		HashSet<MeasurementName> commonNames = new HashSet<>(firstMap.keySet());
		commonNames.retainAll(secondMap.keySet());

		ArrayList<DimensionDifference> differences = new ArrayList<>();
		for (MeasurementName name : commonNames) {
			MeasurementValues firstValues = firstMap.get(name);
			MeasurementValues secondValues = secondMap.get(name);

			// TODO use actual dimension
			Dimension dimension = new Dimension(name, Unit.DEFAULT, Interpretation.DEFAULT);

			DimensionDifference difference = new DimensionDifference(
				significanceFactors,
				dimension,
				firstValues.getAverageValue(),
				secondValues.getAverageValue(),
				getStddev(secondValues).orElse(null)
			);
			differences.add(difference);
		}

		return new RunComparison(first, second, differences);
	}

	private Map<MeasurementName, MeasurementValues> getMeasurementsMap(Run run) {
		Optional<Collection<Measurement>> right = run.getResult().getRight();
		if (right.isEmpty()) {
			return Map.of();
		}

		HashMap<MeasurementName, MeasurementValues> measurements = new HashMap<>();
		for (Measurement measurement : right.get()) {
			Optional<MeasurementValues> content = measurement.getContent().getRight();
			content.ifPresent(
				measurementValues -> measurements.put(measurement.getMeasurementName(), measurementValues)
			);
		}

		return measurements;
	}

	private Optional<Double> getStddev(MeasurementValues measurementValues) {
		if (measurementValues.getValues().size() < significanceFactors.getMinStddevAmount()) {
			return Optional.empty();
		}

		return measurementValues.getStddev();
	}
}
