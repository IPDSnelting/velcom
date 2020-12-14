package de.aaaaaaah.velcom.backend.data.repocomparison.grouping;

import java.time.ZonedDateTime;
import java.time.temporal.JulianFields;

/**
 * Group timestamps by their day. A new day begins at midnight.
 */
public class GroupByDay implements CommitGrouper<Long> {

	@Override
	public Long getGroup(ZonedDateTime time) {
		return time.getLong(JulianFields.JULIAN_DAY);
	}

}
