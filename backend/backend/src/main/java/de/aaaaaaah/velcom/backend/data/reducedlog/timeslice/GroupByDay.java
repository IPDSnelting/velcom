package de.aaaaaaah.velcom.backend.data.reducedlog.timeslice;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.JulianFields;

public class GroupByDay implements CommitGrouper<Long> {

	@Override
	public Long getGroup(ZonedDateTime time) {
		return time.getLong(JulianFields.JULIAN_DAY);
	}
}
