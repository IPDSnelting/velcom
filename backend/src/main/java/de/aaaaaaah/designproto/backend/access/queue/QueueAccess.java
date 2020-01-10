package de.aaaaaaah.designproto.backend.access.queue;

import de.aaaaaaah.designproto.backend.access.AccessLayer;
import de.aaaaaaah.designproto.backend.access.commit.Commit;
import de.aaaaaaah.designproto.backend.access.commit.CommitHash;
import de.aaaaaaah.designproto.backend.access.repo.RepoId;
import de.aaaaaaah.designproto.backend.storage.db.DatabaseStorage;
import java.util.Collection;

/**
 * Those commits which were manually added to the queue are stored in the db. This class abstracts
 * access to those commits. The other commits can be restored after a restart by looking at their
 * {@link Commit#requiresBenchmark()} values.
 *
 * <p> Tasks also store the time when they were created. That information can later be used by the
 * queue policy, for example when reloading tasks after a restart.
 */
public class QueueAccess {

	private final AccessLayer accessLayer;
	private final DatabaseStorage databaseStorage;

	/**
	 * This constructor also registers the {@link QueueAccess} in the accessLayer.
	 *
	 * @param accessLayer the {@link AccessLayer} to register with
	 * @param databaseStorage a database storage
	 */
	public QueueAccess(AccessLayer accessLayer, DatabaseStorage databaseStorage) {
		this.accessLayer = accessLayer;
		this.databaseStorage = databaseStorage;

		accessLayer.registerQueueAccess(this);
	}

	public Collection<Task> getManualTasks() {
		// TODO implement
		return null;
	}

	/**
	 * Add a new manual task to the db.
	 *
	 * @param repoId the repo the commit is in
	 * @param commitHash the commit to benchmark
	 * @return the newly created and added task
	 */
	public Task addManualTask(RepoId repoId, CommitHash commitHash) {
		// TODO implement
		return null;
	}

	/**
	 * Helper wrapper for {@link #addManualTask(RepoId, CommitHash)}.
	 *
	 * @param commit the commit to benchmark
	 * @return the resulting task
	 */
	public Task addManualTask(Commit commit) {
		return addManualTask(commit.getRepoId(), commit.getHash());
	}

	/**
	 * Delete an existing manual task from the db.
	 *
	 * @param repoId the repo the commit is in
	 * @param commitHash the commit the task is for
	 */
	public void deleteManualTask(RepoId repoId, CommitHash commitHash) {
		// TODO implement
	}

	/**
	 * Helper wrapper for {@link #deleteManualTask(RepoId, CommitHash)}.
	 *
	 * @param task the task to delete
	 */
	public void deleteManualTask(Task task) {
		deleteManualTask(task.getRepoId(), task.getCommitHash());
	}

}
