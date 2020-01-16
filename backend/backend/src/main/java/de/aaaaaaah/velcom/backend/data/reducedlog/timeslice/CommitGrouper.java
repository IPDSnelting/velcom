package de.aaaaaaah.velcom.backend.data.reducedlog.timeslice;

import java.time.LocalTime;

interface CommitGrouper<T> {

	T getGroup(LocalTime time);
}
