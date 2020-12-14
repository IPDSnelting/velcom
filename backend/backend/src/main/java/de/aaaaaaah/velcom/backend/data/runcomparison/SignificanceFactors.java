package de.aaaaaaah.velcom.backend.data.runcomparison;

/**
 * A few factors describing how the standard deviation and significance of a run should be
 * determined.
 */
public class SignificanceFactors {

	private final double relativeThreshold;
	private final double stddevThreshold;
	private final int minStddevAmount;

	public SignificanceFactors(double relativeThreshold, double stddevThreshold,
		int minStddevAmount) {

		if (relativeThreshold < 0) {
			throw new IllegalArgumentException("relative threshold must be >= 0");
		}
		if (stddevThreshold < 0) {
			throw new IllegalArgumentException("stddev threshold must be >= 0");
		}
		if (minStddevAmount < 2) {
			throw new IllegalArgumentException("minimum stddev amount must be at least 2");
		}

		this.relativeThreshold = relativeThreshold;
		this.stddevThreshold = stddevThreshold;
		this.minStddevAmount = minStddevAmount;
	}

	public double getRelativeThreshold() {
		return relativeThreshold;
	}

	public double getStddevThreshold() {
		return stddevThreshold;
	}

	public int getMinStddevAmount() {
		return minStddevAmount;
	}
}
