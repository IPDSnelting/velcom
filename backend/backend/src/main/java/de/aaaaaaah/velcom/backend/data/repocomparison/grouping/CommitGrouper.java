package de.aaaaaaah.velcom.backend.data.repocomparison.grouping;

import java.time.ZonedDateTime;

/**
 * Group timestamps using arbitrary grouping logic. Calculates an arbitrary group name. Timestamps
 * where the same group name was returned belong in the same group.
 *
 * @param <T> type of the group name
 */
public interface CommitGrouper<T> {

	/**
	 * Calculate the group name for a timestamp.
	 *
	 * @param time the timestamp whose group name to calculate
	 * @return the timestamp's group name
	 */
	T getGroup(ZonedDateTime time);

}
