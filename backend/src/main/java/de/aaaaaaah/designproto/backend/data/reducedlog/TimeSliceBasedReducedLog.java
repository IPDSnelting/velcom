package de.aaaaaaah.designproto.backend.data.reducedlog;

import de.aaaaaaah.designproto.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.designproto.backend.access.commit.Commit;
import java.util.Collection;
import java.util.List;

/**
 * This reduction strategy groups the commits into slices of time (e.g. hours or days) and selects
 * the best performing commit for each time slice. Drops all commits that don't have a successful
 * measurement of the specified name.
 */
public class TimeSliceBasedReducedLog implements ReducedLog {

	@Override
	public List<Commit> reduce(Collection<Commit> commits, MeasurementName measurementName) {
		// TODO implement
		return null;
	}
}
