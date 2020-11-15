package de.aaaaaaah.velcom.backend.data.queue;

import static java.util.stream.Collectors.toList;

import de.aaaaaaah.velcom.backend.access.BenchmarkWriteAccess;
import de.aaaaaaah.velcom.backend.access.entities.benchmark.NewRun;
import de.aaaaaaah.velcom.backend.newaccess.archiveaccess.ArchiveReadAccess;
import de.aaaaaaah.velcom.backend.newaccess.archiveaccess.exceptions.TarRetrieveException;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.TaskWriteAccess;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskPriority;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.exceptions.NoSuchTaskException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * The queue keeps track of all current tasks and their priority and status. It knows which tasks
 * are currently in progress and the order in which the remaining tasks should be worked on, based
 * on their priority and other info. For more info on the task order, see the {@link Policy}.
 */
public class Queue {

	private final TaskWriteAccess taskAccess;
	private final ArchiveReadAccess archiveAccess;
	private final BenchmarkWriteAccess benchAccess;

	private final AtomicReference<Optional<RepoId>> currentRepoId;

	public Queue(TaskWriteAccess taskAccess, ArchiveReadAccess archiveAccess,
		BenchmarkWriteAccess benchAccess) {

		this.taskAccess = taskAccess;
		this.archiveAccess = archiveAccess;
		this.benchAccess = benchAccess;

		currentRepoId = new AtomicReference<>(Optional.empty());
	}

	/**
	 * Gets the task with the specified id.
	 *
	 * @param taskId the id of the task
	 * @return the task
	 * @throws NoSuchTaskException if no task with the specified id exists
	 */
	public Task getTask(TaskId taskId) throws NoSuchTaskException {
		return taskAccess.getTask(taskId);
	}

	/**
	 * @return all tasks currently in the queue. First come the tasks which are currently in progress,
	 * 	in no particular order. Then come the tasks which are currently not in progress, in the order
	 * 	they would be returned by successive calls to {@link #startNextTask()}.
	 */
	public List<Task> getAllTasksInOrder() {
		List<Task> allTasks = taskAccess.getAllTasks();

		List<Task> result = allTasks.stream()
			.filter(Task::isInProgress)
			.collect(Collectors.toCollection(ArrayList::new));

		List<Task> tasksNotInProgress = allTasks.stream()
			.filter(task -> !task.isInProgress())
			.collect(toList());

		Policy policy = new Policy(tasksNotInProgress, currentRepoId.get().orElse(null));
		result.addAll(policy.stepAll());

		return result;
	}

	/**
	 * Find and start the task that is next in line. The returned task will be marked as "in progress"
	 * until it is completed via {@link #completeTask(NewRun)} or aborted via {@link
	 * #abortTask(TaskId)}.
	 *
	 * @return the task that was started
	 */
	public Optional<Task> startNextTask() {
		return taskAccess.startTask(tasks -> {
			Policy policy = new Policy(tasks, currentRepoId.get().orElse(null));
			Optional<Task> nextTask = policy.step();

			if (nextTask.isPresent()) {
				currentRepoId.set(policy.getCurrentRepoId());
			}

			return nextTask.map(Task::getId);
		});
	}

	/**
	 * Convert the task with the same id as the {@link NewRun} into a full run. This removes the task
	 * from the queue and adds the newly created run into the database.
	 *
	 * @param result the result associated with the task
	 */
	public void completeTask(NewRun result) {
		benchAccess.insertRun(result);
	}

	/**
	 * Cancel the processing of the task with the given task id. This merely marks the task as "not in
	 * progress" and will not delete the task from the queue.
	 *
	 * @param taskId the id of the task
	 */
	public void abortTask(TaskId taskId) {
		taskAccess.setTaskInProgress(taskId, false);
	}

	/**
	 * Delete multiple from the queue by their id.
	 *
	 * @param taskIds the ids of the tasks
	 */
	public void deleteTasks(Collection<TaskId> taskIds) {
		taskAccess.deleteTasks(taskIds);
	}

	/**
	 * Delete all tasks from this queue.
	 */
	public void deleteAllTasks() {
		taskAccess.deleteAllTasks();
	}

	/**
	 * A good way to check whether a task exists and is in progress.
	 *
	 * @param taskId the id of the task to check for
	 * @return true if the task exists and is in progress, false otherwise
	 */
	public boolean isTaskInProgress(TaskId taskId) {
		return taskAccess.isTaskInProgress(taskId).orElse(false);
	}

	/**
	 * Add a commit to the queue if no task for this commit already exists.
	 *
	 * @param author the new task's author
	 * @param repoId the id of the repo the commit belongs to
	 * @param hash the commit hash
	 * @param priority the new task's priority
	 * @return the new task or empty if there already was a task for this commit in the queue
	 */
	public Optional<Task> addCommit(String author, RepoId repoId, CommitHash hash,
		TaskPriority priority) {

		return taskAccess.insertCommit(author, priority, repoId, hash);
	}

	/**
	 * Add multiple commits to the queue in order. Does not add commits where a corresponding task
	 * already exists in the queue.
	 *
	 * @param author the new tasks' author
	 * @param repoId the id of the repo the commits belong to
	 * @param hashes the commit hashes
	 * @param priority the new tasks' priority
	 */
	public void addCommits(String author, RepoId repoId, List<CommitHash> hashes,
		TaskPriority priority) {

		taskAccess.insertCommits(author, priority, repoId, hashes);
	}

	/**
	 * Changes the priority of the task with the specified task id.
	 *
	 * @param taskId the id of the task
	 * @param newPriority the new priority
	 */
	public void prioritizeTask(TaskId taskId, TaskPriority newPriority) {
		taskAccess.setTaskPriority(taskId, newPriority);
	}

	/**
	 * Transfers a task to the supplied {@link OutputStream}.
	 *
	 * <p> Note that the provided output stream will be closed after the transfer operation is
	 * done.
	 *
	 * @param taskId the id of the task to be transferred
	 * @param output the output to transfer the task to
	 * @throws NoSuchTaskException if no task with the given id exists
	 * @throws TarRetrieveException if the tar file could not be retrieved or transferred
	 */
	public void transferTask(TaskId taskId, OutputStream output)
		throws NoSuchTaskException, TarRetrieveException {

		Task task = taskAccess.getTask(taskId);
		archiveAccess.transferTask(task, output);
	}

}
