package de.aaaaaaah.velcom.backend.data.commitcomparison;

import de.aaaaaaah.velcom.backend.access.benchmark.Run;
import java.util.Collection;
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

	public Collection<CommitDifference> getDifferences() {
		// TODO implement
		return null;
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
