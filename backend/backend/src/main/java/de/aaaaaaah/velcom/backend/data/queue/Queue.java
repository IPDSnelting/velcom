package de.aaaaaaah.velcom.backend.data.queue;

import de.aaaaaaah.velcom.backend.access.commit.BenchmarkStatus;
import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The queue is passed tasks from various sources. It keeps track of all tasks and updates their
 * benchmark status in the {@link CommitAccess}, and also passes all tasks to an internal {@link
 * QueuePolicy}. That policy keeps the tasks and decides in which order they should be executed.
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

	private final CommitAccess commitAccess;
	private final QueuePolicy queuePolicy;

	private final Collection<Consumer<Commit>> somethingAddedListeners;
	private final Collection<Consumer<Pair<RepoId, CommitHash>>> somethingAbortedListeners;

	public Queue(CommitAccess commitAccess, QueuePolicy queuePolicy) {
		this.commitAccess = commitAccess;
		this.queuePolicy = queuePolicy;

		somethingAddedListeners = new ArrayList<>();
		somethingAbortedListeners = new ArrayList<>();
	}

	/**
	 * Add a new non-manual task to the queue. This ignores and overwrites any existing {@link
	 * BenchmarkStatus}.
	 *
	 * @param commit the commit that should be added as task
	 */
	public synchronized void addTask(Commit commit) {
		commitAccess.setBenchmarkStatus(commit.getRepoId(), commit.getHash(),
			BenchmarkStatus.BENCHMARK_REQUIRED);
		queuePolicy.addTask(commit);
		callAllAddedListeners(commit);
	}

	/**
	 * Add a new manual task to the queue. Usually, manual tasks have a higher priority than other
	 * tasks, though this is up to the queue policy. This ignores and overwrites any existing {@link
	 * BenchmarkStatus}.
	 *
	 * @param commit the commit that should be added as task
	 */
	public synchronized void addManualTask(Commit commit) {
		commitAccess.setBenchmarkStatus(commit.getRepoId(), commit.getHash(),
			BenchmarkStatus.BENCHMARK_REQUIRED_MANUAL_PRIORITY);
		queuePolicy.addManualTask(commit);
		callAllAddedListeners(commit);
	}

	/**
	 * Add a commit either as a task or a manual task, based on its {@link
	 * Commit#getBenchmarkStatus()}.
	 *
	 * @param commit the commit that should be added as a task
	 */
	public synchronized void addCommit(Commit commit) {
		if (commit.getBenchmarkStatus()
			.equals(BenchmarkStatus.BENCHMARK_REQUIRED_MANUAL_PRIORITY)) {

			addManualTask(commit);
		} else {
			addTask(commit);
		}
	}

	public synchronized Optional<Commit> getNextTask() {
		return queuePolicy.getNextTask();
	}

	/**
	 * This function must be called once a task was completed and should not be benchmarked again.
	 *
	 * @param commit the commit that was completed
	 */
	public synchronized void finishTask(Commit commit) {
		commitAccess.setBenchmarkStatus(commit.getRepoId(), commit.getHash(),
			BenchmarkStatus.NO_BENCHMARK_REQUIRED);
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
		commitAccess.setBenchmarkStatus(repoId, commitHash, BenchmarkStatus.NO_BENCHMARK_REQUIRED);
		callAllAbortedListeners(repoId, commitHash);
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
