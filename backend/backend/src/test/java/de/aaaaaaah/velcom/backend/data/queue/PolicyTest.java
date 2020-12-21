package de.aaaaaaah.velcom.backend.data.queue;

import static org.assertj.core.api.Assertions.assertThat;

import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.sources.CommitSource;
import de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities.sources.TarSource;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.shared.util.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PolicyTest {

	private final String description = "test description";
	private final CommitHash hash = new CommitHash("a2ec9e64ca2a4243a15554a2678e6af95ce97b7a");

	private RepoId repo1;
	private RepoId repo2;
	private RepoId repo3;
	private RepoId repo4;

	@BeforeEach
	void setUp() {
		repo1 = new RepoId(UUID.fromString("2ea6e24a-ef7e-4cac-bb08-054b0e24473a"));
		repo2 = new RepoId(UUID.fromString("6bed466a-0a48-4f14-a708-3ac8c02abd72"));
		repo3 = new RepoId(UUID.fromString("8b8a568b-24e4-433e-9c6c-ec97a63d84ed"));
		repo4 = new RepoId(UUID.fromString("cfa4de7c-8fb2-4f80-8998-94c81586b756"));
	}

	private Task comTask(String author, TaskPriority priority, RepoId repoId) {
		return new Task(author, priority, Either.ofLeft(new CommitSource(repoId, hash)));
	}

	private Task tarTask(String author, TaskPriority priority, @Nullable RepoId repoId) {
		return new Task(author, priority, Either.ofRight(new TarSource(description, repoId)));
	}

	// Use the policy's built-in function for getting all tasks in order
	private List<Task> useStepAll(List<Task> tasks, @Nullable RepoId repoId) {
		return new Policy(tasks, repoId).stepAll();
	}

	// Try to simulate starting tasks one by one via the Queue
	private List<Task> simulQueue(List<Task> tasks, @Nullable RepoId repoId) {
		List<Task> currentTasks = new ArrayList<>(tasks);
		Optional<RepoId> currentRepoId = Optional.ofNullable(repoId);

		List<Task> result = new ArrayList<>();

		while (true) {
			Policy policy = new Policy(currentTasks, currentRepoId.orElse(null));
			Optional<Task> task = policy.step();

			if (task.isPresent()) {
				result.add(task.get());
				currentTasks.removeIf(t -> t.getId().equals(task.get().getId()));
				currentRepoId = policy.getCurrentRepoId();
			} else {
				break;
			}
		}

		return result;
	}

	@Test
	void manualTasksInFiloOrder() {
		Task task1 = comTask("task1", TaskPriority.MANUAL, repo1);
		Task task2 = tarTask("task2", TaskPriority.MANUAL, null);
		Task task3 = comTask("task3", TaskPriority.MANUAL, repo2);
		Task task4 = tarTask("task4", TaskPriority.MANUAL, repo1);

		List<Task> tasks = List.of(task3, task2, task4, task1);
		List<Task> expected = List.of(task4, task3, task2, task1);

		assertThat(useStepAll(tasks, null)).isEqualTo(expected);
		assertThat(simulQueue(tasks, null)).isEqualTo(expected);
	}

	@Test
	void tarTasksInFifoOrder() {
		Task task1 = tarTask("task1", TaskPriority.TAR, repo1);
		Task task2 = tarTask("task2", TaskPriority.TAR, null);
		Task task3 = tarTask("task3", TaskPriority.TAR, repo2);
		Task task4 = tarTask("task4", TaskPriority.TAR, repo1);

		List<Task> tasks = List.of(task3, task2, task4, task1);
		List<Task> expected = List.of(task1, task2, task3, task4);

		assertThat(useStepAll(tasks, null)).isEqualTo(expected);
		assertThat(simulQueue(tasks, null)).isEqualTo(expected);
	}

	@Test
	void listenerTasksInFiloOrder() {
		Task task1 = comTask("task1", TaskPriority.LISTENER, repo1);
		Task task2 = comTask("task2", TaskPriority.LISTENER, repo1);
		Task task3 = comTask("task3", TaskPriority.LISTENER, repo1);
		Task task4 = comTask("task4", TaskPriority.LISTENER, repo1);

		List<Task> tasks = List.of(task3, task2, task4, task1);
		List<Task> expected = List.of(task4, task3, task2, task1);

		assertThat(useStepAll(tasks, null)).isEqualTo(expected);
		assertThat(simulQueue(tasks, null)).isEqualTo(expected);
	}

	@Test
	void listenerTasksRoundRobinAndFilo() {
		Task r1t1 = comTask("r1t1", TaskPriority.LISTENER, repo1);
		Task r1t2 = comTask("r1t2", TaskPriority.LISTENER, repo1);
		Task r1t3 = comTask("r1t3", TaskPriority.LISTENER, repo1);
		Task r1t4 = comTask("r1t4", TaskPriority.LISTENER, repo1);

		Task r2t1 = comTask("r2t1", TaskPriority.LISTENER, repo2);

		Task r3t1 = comTask("r3t1", TaskPriority.LISTENER, repo3);
		Task r3t2 = comTask("r3t2", TaskPriority.LISTENER, repo3);
		Task r3t3 = comTask("r3t3", TaskPriority.LISTENER, repo3);

		List<Task> tasks = List.of(r1t3, r3t2, r2t1, r1t1, r1t2, r3t3, r3t1, r1t4);
		List<Task> expected = List.of(r1t4, r2t1, r3t3, r1t3, r3t2, r1t2, r3t1, r1t1);

		assertThat(useStepAll(tasks, repo1)).isEqualTo(expected);
		assertThat(simulQueue(tasks, repo1)).isEqualTo(expected);
	}

	@Test
	void manualBeforeTarBeforeListener() {
		Task m1 = comTask("m1", TaskPriority.MANUAL, repo1);
		Task m2 = tarTask("m2", TaskPriority.MANUAL, null);

		Task t1 = tarTask("t1", TaskPriority.TAR, repo2);
		Task t2 = tarTask("t2", TaskPriority.TAR, null);

		Task l1 = comTask("l1", TaskPriority.LISTENER, repo1);
		Task l2 = comTask("l2", TaskPriority.LISTENER, repo2);
		Task l3 = comTask("l3", TaskPriority.LISTENER, repo2);

		List<Task> tasks = List.of(l2, t1, t2, m2, l3, m1, l1);
		List<Task> expected = List.of(m2, m1, t1, t2, l1, l3, l2);

		assertThat(useStepAll(tasks, repo1)).isEqualTo(expected);
		assertThat(simulQueue(tasks, repo1)).isEqualTo(expected);
	}

	@Test
	void findCorrectNextRepo() {
		Task r1 = comTask("r1", TaskPriority.LISTENER, repo1);
		Task r2 = comTask("r2", TaskPriority.LISTENER, repo2);
		Task r4 = comTask("r4", TaskPriority.LISTENER, repo4);

		List<Task> tasks = List.of(r4, r2, r1);

		List<Task> expected1 = List.of(r1, r2, r4);
		assertThat(useStepAll(tasks, null)).isEqualTo(expected1);
		assertThat(simulQueue(tasks, null)).isEqualTo(expected1);

		List<Task> expected2 = List.of(r2, r4, r1);
		assertThat(useStepAll(tasks, repo2)).isEqualTo(expected2);
		assertThat(simulQueue(tasks, repo2)).isEqualTo(expected2);

		List<Task> expected3 = List.of(r4, r1, r2);
		assertThat(useStepAll(tasks, repo3)).isEqualTo(expected3);
		assertThat(simulQueue(tasks, repo3)).isEqualTo(expected3);
	}
}
