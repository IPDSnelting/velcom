package de.aaaaaaah.velcom.backend.data.queue;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import de.aaaaaaah.velcom.backend.ServerMain;
import de.aaaaaaah.velcom.backend.newaccess.KnownCommitWriteAccess;
import de.aaaaaaah.velcom.backend.newaccess.entities.BenchmarkStatus;
import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import de.aaaaaaah.velcom.backend.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The queue is passed tasks from various sources. It keeps track of all tasks and updates their
 * benchmark status in the {@link KnownCommitWriteAccess}, and also passes all tasks to an internal
 * {@link QueuePolicy}. That policy keeps the tasks and decides in which order they should be
 * executed.
 *
 * <p> When the queue is loaded after a restart, all the known commits which still need to be
 * benchmarked as well as the manual commits need to be added one by one again. The queue does
 * <em>>not</em> do this itself.
 *
 * <p> All publicly available functions in the queue are synchronized, which should make the queue
 * threadsafe to use.
 *
 * <p> The policy must follow these rules:
 * <ul>
 *     <li>There is always at most one task per commit.</li>
 *     <li>The policy must never drop or ignore a commit.</li>
 *     <li>If a manual task is added for a commit, that always replaces a previous non-manual task.</li>
 *     <li>A non-manual task never replaces a manual task.</li>
 * </ul>
 */
public class Queue {

	private static final Logger LOGGER = LoggerFactory.getLogger(Queue.class);

	private final KnownCommitWriteAccess knownCommitAccess;
	private final QueuePolicy queuePolicy;

	private final Collection<Consumer<Commit>> somethingAddedListeners;
	private final Collection<Consumer<Pair<RepoId, CommitHash>>> somethingAbortedListeners;

	public Queue(KnownCommitWriteAccess knownCommitAccess, QueuePolicy queuePolicy) {

		this.knownCommitAccess = knownCommitAccess;
		this.queuePolicy = queuePolicy;

		somethingAddedListeners = new ArrayList<>();
		somethingAbortedListeners = new ArrayList<>();

		ServerMain.getMetricRegistry().register(
			MetricRegistry.name(getClass(), "queue_length"),
			(Gauge<Integer>) () -> viewAllCurrentTasks().size()
		);
	}

	/**
	 * Add a new non-manual task to the queue. This ignores and overwrites any existing {@link
	 * BenchmarkStatus}.
	 *
	 * @param commit the commit that should be added as task
	 */
	public synchronized void addTask(Commit commit) {
		if (queuePolicy.addTask(commit)) {
			knownCommitAccess.setBenchmarkStatus(commit.getRepoId(), commit.getHash(),
				BenchmarkStatus.BENCHMARK_REQUIRED);
			callAllAddedListeners(commit);
			LOGGER.info("Added task " + commit + " to queue and notified all listeners");
		} else {
			LOGGER.debug("Didn't add task " + commit + " as it was already added earlier");
		}
	}

	/**
	 * Add a new manual task to the queue. Usually, manual tasks have a higher priority than other
	 * tasks, though this is up to the queue policy. This ignores and overwrites any existing {@link
	 * BenchmarkStatus}.
	 *
	 * @param commit the commit that should be added as task
	 */
	public synchronized void addManualTask(Commit commit) {
		if (queuePolicy.addManualTask(commit)) {
			knownCommitAccess.setBenchmarkStatus(commit.getRepoId(), commit.getHash(),
				BenchmarkStatus.BENCHMARK_REQUIRED_MANUAL_PRIORITY);
			callAllAddedListeners(commit);
			LOGGER.info("Added manual task " + commit + " to queue and notified all listeners");
		} else {
			LOGGER.debug("Didn't add manual task " + commit + " as it was already added earlier");
		}
	}

	/**
	 * Add a commit either as a task or a manual task, based on its benchmark status.
	 *
	 * @param commit the commit that should be added as a task
	 */
	public synchronized void addCommit(Commit commit) {
		BenchmarkStatus status = knownCommitAccess.getBenchmarkStatus(commit.getRepoId(),
			commit.getHash());

		if (status.equals(BenchmarkStatus.BENCHMARK_REQUIRED_MANUAL_PRIORITY)) {
			addManualTask(commit);
		} else {
			addTask(commit);
		}
	}

	public synchronized Optional<Commit> getNextTask() {
		final Optional<Commit> nextTask = queuePolicy.getNextTask();
		nextTask.ifPresent(commit -> LOGGER.info("Task " + commit + " has been started"));
		return nextTask;
	}

	/**
	 * This function must be called once a task was completed and should not be benchmarked again.
	 *
	 * @param repoId the repo the commit is in
	 * @param commitHash the commit's hash
	 */
	public synchronized void finishTask(RepoId repoId, CommitHash commitHash) {
		knownCommitAccess.setBenchmarkStatus(repoId, commitHash,
			BenchmarkStatus.NO_BENCHMARK_REQUIRED);
		LOGGER.info(
			"Task with repoId " + repoId + " and commitHash " + commitHash
				+ " was successfully finished");
	}

	/**
	 * Get all tasks currently in the queue, in the order they are going to be executed in. In other
	 * words, repeated calls to {@link #getNextTask()} will result in this order of tasks, unless
	 * new tasks are added or tasks removed during that time.
	 *
	 * @return the list of tasks in the order they will be executed
	 */
	public synchronized List<Commit> viewAllCurrentTasks() {
		return queuePolicy.viewAllCurrentTasks();
	}

	/**
	 * Abort a commit's task.
	 *
	 * @param repoId the repo the commit is in
	 * @param commitHash the commit's hash
	 */
	public synchronized void abortTask(RepoId repoId, CommitHash commitHash) {
		queuePolicy.abortTask(repoId, commitHash);
		knownCommitAccess.setBenchmarkStatus(
			repoId, commitHash, BenchmarkStatus.NO_BENCHMARK_REQUIRED
		);
		callAllAbortedListeners(repoId, commitHash);
		LOGGER.info("Task with repoId " + repoId + " and hash " + commitHash + " was aborted");
	}

	/**
	 * Remove all tasks of a repo from the queue. This function should only be called <em>after</em>
	 * the repo has already been deleted from the db.
	 *
	 * <p> <b>Warning</b>: This function does <em>not</em> update the database! It only removes
	 * commits from the queue.
	 */
	public synchronized void abortAllTasksOfRepo(RepoId repoId) {
		queuePolicy.abortAllTasksOfRepo(repoId);
		LOGGER.info("All tasks with repoId " + repoId + " were aborted");
	}

	/**
	 * Registers a listener that is called when something is added to the queue.
	 *
	 * @param listener the listener to call if something is added to the queue
	 */
	public synchronized void onSomethingAdded(Consumer<Commit> listener) {
		somethingAddedListeners.add(listener);
	}

	private void callAllAddedListeners(Commit commit) {
		somethingAddedListeners.forEach(taskConsumer -> taskConsumer.accept(commit));
	}

	/**
	 * Registers a listener that is called when a task is aborted.
	 *
	 * @param listener the listener to call if a task is aborted
	 */
	public synchronized void onSomethingAborted(Consumer<Pair<RepoId, CommitHash>> listener) {
		somethingAbortedListeners.add(listener);
	}

	private void callAllAbortedListeners(RepoId repoId, CommitHash commitHash) {
		Pair<RepoId, CommitHash> pair = new Pair<>(repoId, commitHash);
		somethingAbortedListeners.forEach(pairConsumer -> pairConsumer.accept(pair));
	}

}
