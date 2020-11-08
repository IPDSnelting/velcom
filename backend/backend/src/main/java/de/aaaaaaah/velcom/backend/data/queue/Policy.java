package de.aaaaaaah.velcom.backend.data.queue;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;

import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskPriority;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

class Policy {

	private final Queue<Task> manualTasks;
	private final Queue<Task> tarTasks;

	private final Map<RepoId, Queue<Task>> listenerTasksPerRepo;
	private final Queue<RepoId> repos;

	public Policy(List<Task> tasks, @Nullable RepoId currentRepoId) {
		// Tasks ordered from newest to oldest (FILO)
		List<Task> tasksInOrder = new ArrayList<>(tasks);
		tasksInOrder.sort(comparing(Task::getUpdateTime));
		Collections.reverse(tasksInOrder);

		manualTasks = tasksInOrder.stream()
			.filter(task -> task.getPriority().equals(TaskPriority.MANUAL))
			.collect(toCollection(ArrayDeque::new));

		// Tar tasks need to be reversed since they need a FIFO order, not FILO
		ArrayList<Task> tarTasksTmp = tasksInOrder.stream()
			.filter(task -> task.getPriority().equals(TaskPriority.TAR))
			.collect(toCollection(ArrayList::new));
		Collections.reverse(tarTasksTmp);
		tarTasks = new ArrayDeque<>(tarTasksTmp);

		listenerTasksPerRepo = tasksInOrder.stream()
			.filter(task -> task.getPriority().equals(TaskPriority.LISTENER))
			.filter(task -> task.getSource().isLeft())
			.collect(groupingBy(
				task -> task.getSource().getLeft().get().getRepoId(),
				HashMap::new,
				toCollection(ArrayDeque::new)
			));

		repos = listenerTasksPerRepo.keySet().stream()
			.sorted()
			.collect(Collectors.toCollection(ArrayDeque::new));

		if (currentRepoId != null && !repos.isEmpty()) {
			// Rotate the queue such that the current repo id is the first element. If that doesn't exist,
			// use the repo id that would come after the current id. Usually, this would be the first id
			// that's higher than the current id, but if the current id was already the highest id, this
			// would be the smallest id.
			//
			// If all repo ids are smaller than the current id, this for loop will leave the queue in its
			// initial state, which means that the smallest repo id is next (just like specified above).
			for (int i = 0; i < repos.size(); i++) {
				RepoId id = repos.peek();
				if (id.compareTo(currentRepoId) >= 0) {
					break;
				}

				// Rotate queue along by 1
				repos.add(repos.poll());
			}
		}
	}

	public Optional<Task> step() {
		if (!manualTasks.isEmpty()) {
			return Optional.of(manualTasks.poll());
		}

		if (!tarTasks.isEmpty()) {
			return Optional.of(tarTasks.poll());
		}

		if (repos.isEmpty()) {
			return Optional.empty();
		}

		RepoId repo = repos.poll();
		Queue<Task> repoQueue = listenerTasksPerRepo.get(repo);

		// This should always give back a task since we don't put repos with an empty task queue back
		// into the repo queue and the grouping collector in the constructor doesn't create empty task
		// queues either.
		Task nextTask = repoQueue.remove();

		if (!repoQueue.isEmpty()) {
			// We only add the repo back into the repos queue if it still has at least one task. See
			// comment above for more info.
			repos.add(repo);
		}

		return Optional.of(nextTask);
	}

	public List<Task> stepAll() {
		List<Task> tasksInOrder = new ArrayList<>();

		while (true) {
			Optional<Task> nextTask = step();
			if (nextTask.isPresent()) {
				tasksInOrder.add(nextTask.get());
			} else {
				break;
			}
		}

		return tasksInOrder;
	}

	public Optional<RepoId> getCurrentRepoId() {
		return Optional.ofNullable(repos.peek());
	}
}
