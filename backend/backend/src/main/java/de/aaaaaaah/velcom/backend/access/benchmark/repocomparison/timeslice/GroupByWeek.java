package de.aaaaaaah.velcom.backend.access.benchmark.repocomparison.timeslice;

import java.time.ZonedDateTime;
import java.time.temporal.JulianFields;

public class GroupByWeek implements CommitGrouper<Long> {

	@Override
	public Long getGroup(ZonedDateTime time) {
		return time.getLong(JulianFields.JULIAN_DAY) / 7;
	}
}
