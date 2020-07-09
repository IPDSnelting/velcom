package de.aaaaaaah.velcom.backend.runner_new;

import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.RunId;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.runner_new.single.TeleRunner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The dispatcher interface.
 */
public class Dispatcher {

	private final Map<RunId, TeleRunner> workToRunnerMap;
	private final List<TeleRunner> teleRunners;

	public Dispatcher() {
		this.teleRunners = new ArrayList<>();
		this.workToRunnerMap = new HashMap<>();
	}

	/**
	 * Adds a runner to the dispatcher.
	 *
	 * @param teleRunner the runner to add
	 */
	public boolean addRunner(TeleRunner teleRunner) {
		synchronized (teleRunners) {
			boolean alreadyKnown = teleRunners.stream()
				.map(TeleRunner::getRunnerName)
				.anyMatch(it -> it.equals(teleRunner.getRunnerName()));

			if (alreadyKnown) {
				return false;
			}

			teleRunners.add(teleRunner);
		}
		return true;
	}

	/**
	 * Aborts a given commit if it is currently being executed by a runner.
	 *
	 * @param runId the id of the run
	 * @return true if the commit was aborted, false if it wasn't being executed
	 */
	public boolean abort(RunId runId) {
		synchronized (workToRunnerMap) {
			TeleRunner runner = workToRunnerMap.remove(runId);

			if (runner == null) {
				return false;
			}

			// TODO: 09.07.20 Should this be done on a new thread? 
			runner.abort();

			return true;
		}
	}

	/**
	 * Returns a list with all known runners.
	 *
	 * @return a list with all known runners
	 */
	List<KnownRunner> getKnownRunners() {
		synchronized (teleRunners) {
			return teleRunners.stream()
				.map(TeleRunner::getRunnerInformation)
				.collect(Collectors.toList());
		}
	}

	/**
	 * Returns the {@link TeleRunner} for a given name.
	 *
	 * @param name the name of the runner
	 * @return the runner or an empty optional otherwise
	 */
	Optional<TeleRunner> getTeleRunner(String name) {
		return teleRunners.stream()
			.filter(it -> it.getRunnerName().equals(name))
			.findAny();
	}
}
