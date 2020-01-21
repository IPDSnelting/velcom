package de.aaaaaaah.velcom.backend.data.reducedlog.timeslice;

import java.time.LocalTime;
import java.time.ZonedDateTime;

interface CommitGrouper<T> {

	T getGroup(ZonedDateTime time);
}
