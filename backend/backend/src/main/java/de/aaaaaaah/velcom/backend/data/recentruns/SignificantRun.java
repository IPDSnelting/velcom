package de.aaaaaaah.velcom.backend.data.recentruns;

import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SignificantRun {

	private final Run run;
	private final List<DimensionDifference> differences;

	public SignificantRun(Run run, List<DimensionDifference> differences) {
		this.run = Objects.requireNonNull(run);
		this.differences = new ArrayList<>(Objects.requireNonNull(differences));
	}

	public Run getRun() {
		return run;
	}

	public List<DimensionDifference> getDifferences() {
		return differences;
	}

	@Override
	public String toString() {
		return "SignificantRun{" +
			"run=" + run +
			", differences=" + differences +
			'}';
	}

}
