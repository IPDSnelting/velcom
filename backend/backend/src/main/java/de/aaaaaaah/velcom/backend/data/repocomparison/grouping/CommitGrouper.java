package de.aaaaaaah.velcom.backend.data.repocomparison.grouping;

import java.time.ZonedDateTime;

public interface CommitGrouper<T> {

	T getGroup(ZonedDateTime time);

}
