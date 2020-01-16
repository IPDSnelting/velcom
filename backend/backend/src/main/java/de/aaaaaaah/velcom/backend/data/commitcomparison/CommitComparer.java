package de.aaaaaaah.velcom.backend.data.commitcomparison;

import de.aaaaaaah.velcom.backend.access.benchmark.Run;
import javax.annotation.Nullable;

public class CommitComparer {

	private final double significantFactor;

	public CommitComparer(double significantFactor) {
		this.significantFactor = Math.abs(significantFactor);
	}

	public CommitComparison compare(@Nullable Run first, @Nullable Run second) {
		return new CommitComparison(significantFactor, first, second);
	}
}
