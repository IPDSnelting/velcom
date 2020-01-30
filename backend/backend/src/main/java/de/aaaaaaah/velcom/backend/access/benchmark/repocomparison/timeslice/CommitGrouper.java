package de.aaaaaaah.velcom.backend.access.benchmark.repocomparison.timeslice;

import java.time.ZonedDateTime;

public interface CommitGrouper<T> {

	T getGroup(ZonedDateTime time);
}
