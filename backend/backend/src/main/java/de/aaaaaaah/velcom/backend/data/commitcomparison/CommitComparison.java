package de.aaaaaaah.velcom.backend.data.commitcomparison;

import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.Measurement;
import de.aaaaaaah.velcom.backend.access.entities.Dimension;
import de.aaaaaaah.velcom.backend.access.entities.MeasurementValues;
import de.aaaaaaah.velcom.backend.access.entities.Run;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
	private final Commit firstCommit;
	@Nullable
	private final Run firstRun;
	private final Commit secondCommit;
	@Nullable
	private final Run secondRun;

	CommitComparison(double significantFactor, @Nullable Commit firstCommit, @Nullable Run firstRun,
		Commit secondCommit, @Nullable Run secondRun) {

		this.significantFactor = significantFactor;
		this.firstCommit = firstCommit;
		this.firstRun = firstRun;
		this.secondCommit = Objects.requireNonNull(secondCommit);
		this.secondRun = secondRun;
	}

	public Optional<Commit> getFirstCommit() {
		return Optional.ofNullable(firstCommit);
	}

	public Optional<Run> getFirstRun() {
		return Optional.ofNullable(firstRun);
	}

	public Commit getSecondCommit() {
		return secondCommit;
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
		if (firstRun == null || firstRun.getResult().isLeft()
			|| secondRun == null || secondRun.getResult().isLeft()) {
			return new ArrayList<>();
		}

		Collection<CommitDifference> commitDifferences = new ArrayList<>();

		// Put the measurements of the second run into a map
		Collection<Measurement> firstMeasurements = firstRun.getResult().getRight().get();
		Collection<Measurement> secondMeasurements = secondRun.getResult().getRight().get();
		Map<Dimension, Measurement> secondMap = new HashMap<>();
		for (Measurement m : secondMeasurements) {
			secondMap.put(m.getDimension(), m);
		}

		// Iterate through fist measurements and check if a measurement by the same name exists in
		// the second run. If so, create a commitDifference and add it to the list
		for (Measurement firstMeasurement : firstMeasurements) {
			final Dimension name = firstMeasurement.getDimension();
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
				firstValues.get().getAverageValue(), secondValues.get().getAverageValue()));
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
