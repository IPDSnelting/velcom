package de.aaaaaaah.velcom.backend.data.recentruns;

import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.Run;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A run that has been found to be significant. Contains the significant {@link
 * DimensionDifference}s which caused this.
 */
public class SignificantRun {

	private final Run run;
	private final List<DimensionDifference> significantDifferences;

	public SignificantRun(Run run, List<DimensionDifference> significantDifferences) {
		this.run = Objects.requireNonNull(run);
		this.significantDifferences = new ArrayList<>(Objects.requireNonNull(significantDifferences));
	}

	public Run getRun() {
		return run;
	}

	public List<DimensionDifference> getSignificantDifferences() {
		return significantDifferences;
	}

	@Override
	public String toString() {
		return "SignificantRun{" +
			"run=" + run +
			", significantDifferences=" + significantDifferences +
			'}';
	}
}
