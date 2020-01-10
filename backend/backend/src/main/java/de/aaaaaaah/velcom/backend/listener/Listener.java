package de.aaaaaaah.velcom.backend.listener;

import de.aaaaaaah.velcom.backend.GlobalConfig;
import de.aaaaaaah.velcom.backend.access.AccessLayer;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoAccess;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A listener periodically checks if there are new commits on stored repositories and, if there are,
 * passes them to the queue.
 */
public class Listener {

	private final RepoAccess repoAccess;
	private final CommitAccess commitAccess;

	private final ScheduledExecutorService executor;

	/**
	 * Constructs a new listener instance.
	 *
	 * @param config the config where the listener gets the poll interval from
	 * @param accessLayer the access layer to get several access instances, that the listener
	 * 	needs, from
	 */
	public Listener(GlobalConfig config, AccessLayer accessLayer) {
		this.repoAccess = accessLayer.getRepoAccess();
		this.commitAccess = accessLayer.getCommitAccess();

		long pollInterval = config.getPollInterval();

		executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(this::update, 0, pollInterval, TimeUnit.SECONDS);
	}

	private void update() {
		// TODO implement
	}

	/**
	 * Checks for new commits on the specified repository and passes the new commits to the queue.
	 *
	 * @param repoId the id of the repository to check for
	 */
	public void checkForUnknownCommits(RepoId repoId) {
		// TODO implement
	}

	/**
	 * Shuts the listener down.
	 */
	public void shutdown() {
		executor.shutdown(); // Disable new tasks from being submitted

		try {
			// Wait a while for existing tasks to terminate
			if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
				executor.shutdownNow(); // Cancel currently executing tasks

				// Wait a while for tasks to respond to being cancelled
				if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
					System.err.println("Pool did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			executor.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

}
