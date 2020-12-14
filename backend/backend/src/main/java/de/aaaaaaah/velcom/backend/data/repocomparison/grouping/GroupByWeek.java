package de.aaaaaaah.velcom.backend.data.repocomparison.grouping;

import java.time.ZonedDateTime;
import java.time.temporal.JulianFields;

/**
 * Group timestamps into slices which are one week long. No guarantee is given which day of the week
 * the slices start and end on.
 */
public class GroupByWeek implements CommitGrouper<Long> {

	@Override
	public Long getGroup(ZonedDateTime time) {
		return time.getLong(JulianFields.JULIAN_DAY) / 7;
	}

}
