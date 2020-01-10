package de.aaaaaaah.velcom.backend.runner;

import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import de.aaaaaaah.velcom.backend.runner.single.ActiveRunnerInformation;
import java.util.List;

/**
 * The dispatcher.
 */
public interface Dispatcher {

	/**
	 * Adds a runner. The runner will automatically be removed when it disconnects.
	 *
	 * @param runnerInformation the runner information of the runner to add
	 */
	void addRunner(ActiveRunnerInformation runnerInformation);

	/**
	 * Called when the queue has changed. This means that the dispatcher might need to fetch new
	 * work to distribute.
	 */
	void onQueueChanged();

	/**
	 * Aborts a given commit if it is currently being executed by a runner.
	 *
	 * @param commitHash the hash of the commit
	 * @param repoId the id of the repo it belongs to
	 * @return true if the commit was aborted, false if it wasn't being executed
	 */
	boolean abort(CommitHash commitHash, RepoId repoId);

	/**
	 * Returns a list with all known runners.
	 *
	 * @return a list with all known runners
	 */
	List<KnownRunner> getKnownRunners();
}
