package de.aaaaaaah.designproto.backend.data.reducedlog;

import de.aaaaaaah.designproto.backend.access.benchmark.MeasurementName;
import de.aaaaaaah.designproto.backend.access.commit.Commit;
import java.util.Collection;
import java.util.List;

/**
 * Reducing means bringing a bunch of commits into a linear order, while possibly ignoring/dropping
 * some of them. It is used to prepare commits for the repo comparison view.
 */
public interface ReducedLog {

	/**
	 * Reduce a bunch of commits into some sort of linearized form using a measurement. This
	 * function may also drop commits.
	 *
	 * @param commits the commits to linearize
	 * @param measurementName what measurement to use for linearizing
	 * @return a linearized form of the input commits (not necessarily all of them though)
	 */
	List<Commit> reduce(Collection<Commit> commits, MeasurementName measurementName);
}
