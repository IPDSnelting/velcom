package de.aaaaaaah.velcom.backend.runner;

import de.aaaaaaah.velcom.shared.util.LinesWithOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The dispatcher interface other modules can compile against.
 */
public interface IDispatcher {

	/**
	 * @return a list with all known runners
	 */
	List<KnownRunner> getKnownRunners();

	/**
	 * @param taskId the task to find the last log lines for
	 * @return the last output lines for a given task. Considers live runners and the last few
	 * 	finished results of each runner.
	 */
	Optional<LinesWithOffset> findLinesForTask(UUID taskId);
}
