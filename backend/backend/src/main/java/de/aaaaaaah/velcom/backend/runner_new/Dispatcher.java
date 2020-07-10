package de.aaaaaaah.velcom.backend.runner_new;

import de.aaaaaaah.velcom.backend.access.entities.Run;
import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.access.entities.TaskId;
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
public class Dispatcher implements IDispatcher {

	private final Map<TaskId, TeleRunner> workToRunnerMap;
	private final List<TeleRunner> teleRunners;
	private final Queue queue;

	public Dispatcher(Queue queue) {
		this.queue = queue;
		this.teleRunners = new ArrayList<>();
		this.workToRunnerMap = new HashMap<>();
	}

	@Override
	public boolean abort(TaskId runId) {
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
	}

	@Override
	public List<KnownRunner> getKnownRunners() {
		synchronized (teleRunners) {
			return teleRunners.stream()
				.map(TeleRunner::getRunnerInformation)
				.collect(Collectors.toList());
		}
	}

	/**
	 * Marks a task as completed.
	 *
	 * @param result the resulting run
	 */
	public void completeTask(Run result) {
		queue.completeTask(result);
	}

	/**
	 * Returns the next work the given {@link TeleRunner} should execute and registers it as working.
	 *
	 * @param runner the runner to execute the work on
	 * @return the next available task.
	 */
	public Optional<Task> getWork(TeleRunner runner) {
		Optional<Task> nextTask = queue.fetchNextTask();
		if (nextTask.isEmpty()) {
			return Optional.empty();
		}

		synchronized (workToRunnerMap) {
			workToRunnerMap.put(nextTask.get().getId(), runner);
		}

		return nextTask;
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

	/**
	 * @return the queue the dispatcher uses
	 */
	public Queue getQueue() {
		return queue;
	}
}
