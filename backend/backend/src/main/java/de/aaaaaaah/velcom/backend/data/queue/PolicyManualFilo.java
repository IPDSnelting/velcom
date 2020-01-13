package de.aaaaaaah.velcom.backend.data.queue;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

/**
 * A policy where manual tasks are ordered with a FiLo principle.
 *
 * <p> If there are manual tasks, those are executed before any other tasks. The last manual task
 * to be added is executed first. The other tasks are grouped by repo and ordered from recent to
 * old. Round-robin is performed on the repos: The most recent task of each repo is executed before
 * moving on to the next repo.
 */
public class PolicyManualFilo implements QueuePolicy {

	private Stack<Commit> manualTasks;
	private Map<RepoId, Stack<Commit>> tasks;
	private List<RepoId> keyByIndex = new ArrayList<>();

	private int index;

	/**
	 * Adds task to stack of its repo and if necessary creates new stack for that repo.
	 *
	 * @param commit the commit that has been added as task
	 */
	@Override
	public void addTask(Commit commit) {
		if (!tasks.containsKey(commit.getRepoId())) {
			tasks.put(commit.getRepoId(), new Stack<>());
			keyByIndex.add(commit.getRepoId());
		} else {
			abortTask(commit.getRepoId(), commit.getHash(), true);
		}
		tasks.get(commit.getRepoId()).push(commit);
	}

	/**
	 * Adds task to manual stack.
	 *
	 * @param commit the commit that has been added as task
	 */
	@Override
	public void addManualTask(Commit commit) {
		abortTask(commit.getRepoId(), commit.getHash());
		manualTasks.push(commit);
	}

	/**
	 * Returns next commit.
	 * <ol>
	 *     <li>Top of manual stack</li>
	 *     <li>Top of next repo stack</li>
	 *     <li>Empty</li>
	 * </ol>
	 */
	@Override
	public Optional<Commit> getNextTask() {
		//Return manual task
		if (!manualTasks.isEmpty()) {
			return Optional.of(manualTasks.pop());
		}

		//If map is empty, return empty
		if (keyByIndex.isEmpty()) {
			return Optional.empty();
		}

		//Iterate to next non-empty repo stack
		int startIndex = index;
		do {
			index = (index + 1) % keyByIndex.size();

			Stack<Commit> stack = tasks.get(keyByIndex.get(index));
			if (!stack.isEmpty()) {
				return Optional.of(stack.pop());
			}
		} while (index != startIndex);

		//If stacks are empty, return empty
		return Optional.empty();
	}

	/**
	 * @return sorted list of all current tasks
	 */
	@Override
	public List<Commit> viewAllCurrentTasks() {
		PolicyManualFilo pmf = copy();
		List<Commit> currentTasks = new ArrayList<>();
		while (true) {
			Optional<Commit> next = pmf.getNextTask();
			if (next.isEmpty()) {
				break;
			}
			currentTasks.add(next.get());
		}
		return currentTasks;
	}

	/**
	 * Removes commit from queue.
	 *
	 * @param repoId the repo the commit is in
	 * @param commitHash the commit hash of the commit to be removed
	 */
	@Override
	public void abortTask(RepoId repoId, CommitHash commitHash) {
		abortTask(repoId, commitHash, false);
	}

	private void abortTask(RepoId repoId, CommitHash commitHash, boolean keepManual) {
		if (!keepManual) {
			removeCommitFromStack(manualTasks, repoId, commitHash);
		}
		Stack<Commit> stack = tasks.get(repoId);
		if (stack != null) {
			removeCommitFromStack(stack, repoId, commitHash);
		}
	}

	//Finds commit in stack by repoId commitHash and removes it
	private void removeCommitFromStack(Stack<Commit> stack, RepoId repoId, CommitHash commitHash) {
		for (Commit commit : stack) {
			if (commit.getRepoId().equals(repoId) && commit.getHash().equals(commitHash)) {
				stack.remove(commit);
				break;
			}
		}
	}

	//Returns copy of this policy
	private PolicyManualFilo copy() {
		PolicyManualFilo pmf = new PolicyManualFilo();

		pmf.manualTasks = new Stack<>();
		manualTasks.forEach(pmf.manualTasks::push);

		pmf.tasks = new HashMap<>(tasks);
		pmf.keyByIndex = new ArrayList<>(keyByIndex);
		pmf.index = index;
		return pmf;
	}

}
