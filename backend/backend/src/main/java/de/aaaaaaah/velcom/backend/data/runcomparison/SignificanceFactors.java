package de.aaaaaaah.velcom.backend.data.runcomparison;

public class SignificanceFactors {

	private final double relativeFactor;
	private final double stddevFactor;
	private final int minStddevAmount;

	public SignificanceFactors(double relativeFactor, double stddevFactor, int minStddevAmount) {
		if (relativeFactor < 0) {
			throw new IllegalArgumentException("relative factor must be >= 0");
		}
		if (stddevFactor < 0) {
			throw new IllegalArgumentException("stddev factor must be >= 0");
		}
		if (minStddevAmount < 2) {
			throw new IllegalArgumentException("minimum stddev amount must be at least 2");
		}

		this.relativeFactor = relativeFactor;
		this.stddevFactor = stddevFactor;
		this.minStddevAmount = minStddevAmount;
	}

	public double getRelativeFactor() {
		return relativeFactor;
	}

	public double getStddevFactor() {
		return stddevFactor;
	}

	public int getMinStddevAmount() {
		return minStddevAmount;
	}
}
