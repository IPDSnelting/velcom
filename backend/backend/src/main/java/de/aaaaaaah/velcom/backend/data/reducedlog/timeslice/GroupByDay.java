package de.aaaaaaah.velcom.backend.data.reducedlog.timeslice;

import java.time.LocalTime;
import java.time.temporal.JulianFields;

public class GroupByDay implements CommitGrouper<Long> {

	@Override
	public Long getGroup(LocalTime time) {
		return time.getLong(JulianFields.JULIAN_DAY);
	}
}
