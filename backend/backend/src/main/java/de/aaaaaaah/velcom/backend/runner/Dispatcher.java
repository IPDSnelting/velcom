package de.aaaaaaah.velcom.backend.runner;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.builder.NewRun;
import de.aaaaaaah.velcom.backend.access.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.data.queue.Queue;
import de.aaaaaaah.velcom.backend.runner.single.TeleRunner;
import de.aaaaaaah.velcom.shared.util.LinesWithOffset;
import de.aaaaaaah.velcom.shared.util.execution.DaemonThreadFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dispatcher implements IDispatcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(Dispatcher.class);

	private final List<TeleRunner> teleRunners;
	private final Queue queue;
	private final Duration disconnectedRunnerGracePeriod;

	public Dispatcher(Queue queue, Duration disconnectedRunnerGracePeriod) {
		this.queue = queue;
		this.disconnectedRunnerGracePeriod = disconnectedRunnerGracePeriod;
		this.teleRunners = new ArrayList<>();

		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
			new DaemonThreadFactory()
		);
		executor.scheduleAtFixedRate(
			this::cleanupDisconnectedRunners,
			0,
			Math.max(disconnectedRunnerGracePeriod.toSeconds() / 2, 1),
			TimeUnit.SECONDS
		);
	}

	private void cleanupDisconnectedRunners() {
		synchronized (teleRunners) {
			LOGGER.debug("Checking for disconnected runners (I know {} runners)", teleRunners.size());
			Predicate<TeleRunner> outOfGracePeriod = runner -> {
				Duration timeSinceLastPing = Duration.between(runner.getLastPing(), Instant.now());
				return timeSinceLastPing.compareTo(disconnectedRunnerGracePeriod) > 0;
			};

			List<TeleRunner> runnersToRemove = new ArrayList<>();

			for (TeleRunner runner : teleRunners) {
				// We synchronize on the runner object. If a runner with this name joins the same object
				// would be returned if the cleanup synchronized block was not yet entered.
				// In this case we would remove the runner from the dispatchers teleRunners list but it
				// would get a new connection and execute work ==> Bad
				// So we mark the runner as disposed and synchronize on it. If the new runner connects
				// while we are here, either we go first and dispose the runner or the listener comes first
				// and sets a new connection. This leads us to bail out of the if and not delete the runner.
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (runner) {
					if (!runner.hasConnection() && outOfGracePeriod.test(runner)) {
						LOGGER.info("Removing disconnected runner {}", runner.getRunnerName());
						runner.dispose();
						runnersToRemove.add(runner);
					}
				}
			}

			teleRunners.removeAll(runnersToRemove);
		}
	}

	/**
	 * Adds a runner to the dispatcher.
	 *
	 * @param teleRunner the runner to add
	 * @throws IllegalArgumentException if the runner name is already taken
	 */
	public void addRunner(TeleRunner teleRunner) {
		synchronized (teleRunners) {
			boolean nameTaken = teleRunners.stream()
				.filter(TeleRunner::hasConnection)
				.map(TeleRunner::getRunnerName)
				.anyMatch(it -> it.equals(teleRunner.getRunnerName()));

			if (nameTaken) {
				throw new IllegalArgumentException("The runner name is already taken!");
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
	public void completeTask(NewRun result) {
		queue.completeTask(result);
	}

	/**
	 * Returns the next work the given {@link TeleRunner} should execute and registers it as working.
	 *
	 * @param runner the runner to execute the work on
	 * @return the next available task.
	 */
	public Optional<Task> getWork(TeleRunner runner) {
		synchronized (teleRunners) {
			// We do not know the runner yet or we know an old version of it apparently.
			Optional<TeleRunner> knownRunner = getTeleRunner(runner.getRunnerName());
			if (knownRunner.isEmpty() || knownRunner.get() != runner) {
				return Optional.empty();
			}
		}
		Optional<Task> nextTask = queue.startNextTask();
		if (nextTask.isEmpty()) {
			return Optional.empty();
		}

		return nextTask;
	}

	public Optional<LinesWithOffset> findLinesForTask(UUID taskId) {
		Optional<KnownRunner> activeWorker = getKnownRunners().stream()
			.filter(it -> it.getCurrentTask().isPresent())
			.filter(it -> it.getCurrentTask().get().getId().getId().equals(taskId))
			.findAny();

		if (activeWorker.isPresent()) {
			LinesWithOffset lastOutputLines = activeWorker.get().getLastOutputLines()
				.orElse(new LinesWithOffset(0, List.of()));
			return Optional.of(lastOutputLines);
		}

		return getKnownRunners().stream()
			.flatMap(it -> it.getCompletedTasks().stream())
			.filter(it -> it.getTaskId().getId().equals(taskId))
			.findFirst()
			.map(it -> it.getLastLogLines().orElse(new LinesWithOffset(0, List.of())));
	}

	/**
	 * Returns the {@link TeleRunner} for a given name.
	 *
	 * @param name the name of the runner
	 * @return the runner or an empty optional otherwise
	 */
	Optional<TeleRunner> getTeleRunner(String name) {
		synchronized (teleRunners) {
			return teleRunners.stream()
				.filter(it -> it.getRunnerName().equals(name))
				.findAny();
		}
	}

	/**
	 * @return the queue the dispatcher uses
	 */
	public Queue getQueue() {
		return queue;
	}
}
