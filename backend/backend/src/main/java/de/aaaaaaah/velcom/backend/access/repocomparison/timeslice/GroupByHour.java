package de.aaaaaaah.velcom.backend.access.repocomparison.timeslice;

import java.time.ZonedDateTime;
import java.time.temporal.JulianFields;

public class GroupByHour implements CommitGrouper<Long> {

	/**
	 * An upper bound on the amount of hours per day.
	 */
	private static final long HOURS_PER_DAY = 25;

	@Override
	public Long getGroup(ZonedDateTime time) {
		return time.getLong(JulianFields.JULIAN_DAY) * HOURS_PER_DAY + time.getHour();
	}
}
