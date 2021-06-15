package de.aaaaaaah.velcom.backend.data.recentruns;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.Run;
import de.aaaaaaah.velcom.backend.data.runcomparison.DimensionDifference;
import de.aaaaaaah.velcom.backend.data.significance.SignificanceReasons;

/**
 * A run that has been found to be significant. Contains the significant {@link
 * DimensionDifference}s which caused this.
 */
public class SignificantRun {

	private final Run run;
	private final SignificanceReasons reasons;

	public SignificantRun(Run run, SignificanceReasons reasons) {
		this.run = run;
		this.reasons = reasons;
	}

	public Run getRun() {
		return run;
	}

	public SignificanceReasons getReasons() {
		return reasons;
	}

	@Override
	public String toString() {
		return "SignificantRun{" +
			"run=" + run +
			", reasons=" + reasons +
			'}';
	}
}
