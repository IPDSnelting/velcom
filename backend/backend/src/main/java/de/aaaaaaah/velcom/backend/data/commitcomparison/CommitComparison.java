package de.aaaaaaah.velcom.backend.data.commitcomparison;

import de.aaaaaaah.velcom.backend.access.benchmark.Measurement;
import de.aaaaaaah.velcom.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.velcom.backend.access.benchmark.Run;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * This class compares a first commit to a second commit. If one or both of the commits are null,
 * the resulting collection of {@link CommitDifference}s is always empty though.
 */
public class CommitComparison {

	@Nullable
	private final Run firstRun;
	@Nullable
	private final Run secondRun;

	public CommitComparison(@Nullable Run firstRun, @Nullable Run secondRun) {
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
	 * Returns list of commitDifferences (of all measurements that exist in both runs)
	 * @return ArrayList of commitDifferences
	 */
	public Collection<CommitDifference> getDifferences() {
		ArrayList<CommitDifference> commitDifferences = new ArrayList<>();
		if ((firstRun == null) || (secondRun == null) || firstRun.getMeasurements().isEmpty()
			|| secondRun.getMeasurements().isEmpty()) {
			return commitDifferences; //Return empty list
		}

		//Get measurements and put those from second run into hashmap
		Collection<Measurement> fm = firstRun.getMeasurements().get();
		Collection<Measurement> sm = secondRun.getMeasurements().get();
		HashMap<MeasurementName, Measurement> secondMeasurements = new HashMap<>();
		for (Measurement m : sm) {
			secondMeasurements.put(m.getMeasurementName(), m);
		}

		//Iterate through measurements from first run and check if a measurement by the same name exists in the second run. If so, create a commitDifference and add it to list
		for (Measurement m : fm) {
			Measurement m2 = secondMeasurements.get(m.getMeasurementName());
			if (m2 != null && m.getContent().isRight() && m2.getContent().isRight()) {
				commitDifferences.add(new CommitDifference(m.getMeasurementName(),
					m2.getContent().getRight().get().getValue() - m.getContent()
						.getRight()
						.get()
						.getValue()));
			}
		}

		return commitDifferences;
	}

	// TODO maybe use cutoff value from config file to determine the significance of commits?

	/**
	 * @return whether the difference is significant enough to appear in the "important news"
	 * 	section of the website.
	 */
	public boolean isSignificant() {
		// TODO implement
		return false;
	}
}
