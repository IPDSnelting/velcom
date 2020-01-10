package de.aaaaaaah.velcom.backend.access.commit;

import java.util.Collection;

/**
 * Allows manual traversal of a commit tree starting at one specific commit.
 */
public class CommitWalk implements AutoCloseable {

	/**
	 * @return the starting commit of this walk
	 */
	public Commit getStart() {
		return null;
	}

	/**
	 * Loads all parents of this commit and returns them in a collection.
	 *
	 * @param child the child commit whose parents are loaded
	 * @return a collection of parent commits of the specified child commit
	 */
	Collection<Commit> getParents(Commit child) {
		return null;
	}

	@Override
	public void close() {
	}

}
