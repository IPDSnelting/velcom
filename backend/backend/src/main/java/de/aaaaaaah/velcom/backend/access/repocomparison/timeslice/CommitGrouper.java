package de.aaaaaaah.velcom.backend.access.repocomparison.timeslice;

import java.time.ZonedDateTime;

public interface CommitGrouper<T> {

	T getGroup(ZonedDateTime time);
}
