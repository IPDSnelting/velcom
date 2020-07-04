package de.aaaaaaah.velcom.backend.runner_new;

import de.aaaaaaah.velcom.backend.runner_new.single.TeleRunner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The dispatcher interface.
 */
public class Dispatcher {

	private final Map<String, UUID> runnerToWorkMap;
	private final List<TeleRunner> teleRunners;

	public Dispatcher() {
		this.teleRunners = new ArrayList<>();
		this.runnerToWorkMap = new HashMap<>();
	}

	/**
	 * Adds a runner to the dispatcher.
	 *
	 * @param teleRunner the runner to add
	 */
	public boolean addRunner(TeleRunner teleRunner) {
		synchronized (teleRunners) {
			boolean alreadyKnown = teleRunners.stream()
				.map(TeleRunner::getRunnerInformation)
				.anyMatch(it -> it.getName().equals(teleRunner.getRunnerInformation().getName()));

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
	boolean abort(UUID runId) {
		synchronized (runnerToWorkMap) {
			// TODO: Send abort
			return runnerToWorkMap.values().remove(runId);
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

}