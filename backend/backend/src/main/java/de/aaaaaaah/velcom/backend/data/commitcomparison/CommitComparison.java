package de.aaaaaaah.velcom.backend.data.commitcomparison;

import de.aaaaaaah.velcom.backend.access.benchmark.Measurement;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.benchmark.Run;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * This class compares a first commit to a second commit. If one or both of the commits are null,
 * the resulting collection of {@link CommitDifference}s is always empty though.
 */
@SuppressWarnings("checkstyle:JavadocStyle")
public class CommitComparison {

	private final double significantFactor;

	@Nullable
	private final Run firstRun;
	@Nullable
	private final Run secondRun;

	CommitComparison(double significantFactor, @Nullable Run firstRun,
		@Nullable Run secondRun) {

		this.significantFactor = significantFactor;
		this.firstRun = firstRun;
		this.secondRun = secondRun;
	}

	public Optional<Run> getFirstRun() {
		return Optional.ofNullable(firstRun);
	}

	public Optional<Run> getSecondRun() {
		return Optional.ofNullable(secondRun);
	}

	/**
	 * Returns list of commitDifferences. (of all measurements that exist in both runs)
	 *
	 * @return ArrayList of commitDifferences
	 */
	public Collection<CommitDifference> getDifferences() {
		if (firstRun == null || firstRun.getMeasurements().isEmpty()
			|| secondRun == null || secondRun.getMeasurements().isEmpty()) {
			return new ArrayList<>();
		}

		Collection<CommitDifference> commitDifferences = new ArrayList<>();

		// Put the measurements of the second run into a map
		Collection<Measurement> firstMeasurements = firstRun.getMeasurements().get();
		Collection<Measurement> secondMeasurements = secondRun.getMeasurements().get();
		Map<MeasurementName, Measurement> secondMap = new HashMap<>();
		for (Measurement m : secondMeasurements) {
			secondMap.put(m.getMeasurementName(), m);
		}

		// Iterate through fist measurements and check if a measurement by the same name exists in
		// the second run. If so, create a commitDifference and add it to the list
		for (Measurement firstMeasurement : firstMeasurements) {
			final MeasurementName name = firstMeasurement.getMeasurementName();
			Measurement secondMeasurement = secondMap.get(name);
			if (secondMeasurement == null) {
				continue;
			}

			Optional<MeasurementValues> firstValues = firstMeasurement.getContent().getRight();
			Optional<MeasurementValues> secondValues = secondMeasurement.getContent().getRight();
			if (firstValues.isEmpty() || secondValues.isEmpty()) {
				continue;
			}

			commitDifferences.add(new CommitDifference(name,
				firstValues.get().getValue(), secondValues.get().getValue()));
		}

		return commitDifferences;
	}

	/**
	 * Checks if difference between two commits is significant.
	 *
	 * @return whether the difference of any measurement is significant enough to appear in the
	 * 	"important news" section of the website.
	 */
	public boolean isSignificant() {
		Collection<CommitDifference> commitDifferences = getDifferences();
		for (CommitDifference difference : commitDifferences) {
			final double previousValue = difference.getFirst();
			final double significanceThreshold = Math.abs(previousValue * significantFactor);
			if (Math.abs(difference.getDifference()) >= significanceThreshold) {
				return true;
			}
		}

		return false;
	}
}
