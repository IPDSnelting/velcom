package de.aaaaaaah.velcom.backend.data.queue;

import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.commit.Task;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.util.List;
import java.util.Optional;

/**
 * A policy where manual tasks are ordered with a FiLo principle.
 *
 * <p> If there are manual tasks, those are executed before any other tasks. The last manual task
 * to be added is executed first. The other tasks are grouped by repo and ordered from recent to
 * old. Round-robin is performed on the repos: The most recent task of each repo is executed before
 * moving on to the next repo.
 */
public class PolicyManualFilo implements QueuePolicy {

	@Override
	public void addTask(Task task) {
		// TODO implement
	}

	@Override
	public void addManualTask(Task task) {
		// TODO implement
	}

	@Override
	public Optional<Task> getNextTask() {
		// TODO implement
		return Optional.empty();
	}

	@Override
	public List<Task> viewAllCurrentTasks() {
		// TODO implement
		return null;
	}

	@Override
	public void abortTask(RepoId repoId, CommitHash commitHash) {
		// TODO implement
	}
}
