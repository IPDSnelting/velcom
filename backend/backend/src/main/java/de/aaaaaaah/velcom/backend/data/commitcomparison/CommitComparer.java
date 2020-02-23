package de.aaaaaaah.velcom.backend.data.commitcomparison;

import de.aaaaaaah.velcom.backend.newaccess.entities.Run;
import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import javax.annotation.Nullable;

public class CommitComparer {

	private final double significantFactor;

	public CommitComparer(double significantFactor) {
		this.significantFactor = Math.abs(significantFactor);
	}

	public CommitComparison compare(@Nullable Commit firstCommit, @Nullable Run firstRun,
		Commit secondCommit, @Nullable Run secondRun) {

		return new CommitComparison(significantFactor, firstCommit, firstRun, secondCommit,
			secondRun);
	}
}
