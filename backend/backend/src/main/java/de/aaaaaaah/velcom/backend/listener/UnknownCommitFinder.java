package de.aaaaaaah.velcom.backend.listener;

import de.aaaaaaah.velcom.backend.access.CommitReadAccess;
import de.aaaaaaah.velcom.backend.access.KnownCommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import java.io.IOException;
import java.util.Collection;

/**
 * Finds all unknown commits that belong to a given branch, which means one of the following
 * statements apply:
 * <ul>
 *     <li>a) the commit is the commit that the branch points at</li>
 *     <li>b) the commit is a (direct or indirect) parent of the commit in a)</li>
 * </ul>
 */
public interface UnknownCommitFinder {

	/**
	 * Finds all unknown commits from the given branch.
	 *
	 * @param access the commit access to gather further information about the branch
	 * @param startCommit the commit to start the search at
	 * @return a collection of unknown commits that belong to the given branch
	 * @throws IOException if an error occurs while trying to find the commits
	 */
	Collection<Commit> find(CommitReadAccess access, KnownCommitReadAccess knownAccess,
		Commit startCommit) throws IOException;

}
