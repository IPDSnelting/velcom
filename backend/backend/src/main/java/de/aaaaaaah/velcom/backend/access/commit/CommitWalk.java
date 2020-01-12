package de.aaaaaaah.velcom.backend.access.commit;

import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Allows manual traversal of a commit tree starting at one specific commit.
 */
public class CommitWalk implements AutoCloseable {

	private final CommitAccess commitAccess;

	private final RepoId repoId;
	private final Repository repo;
	private final RevWalk walk;
	private final Commit startCommit;

	CommitWalk(CommitAccess access, RepoId repoId, Repository repo, RevWalk walk, Commit start) {
		this.commitAccess = access;
		this.repoId = repoId;
		this.repo = repo;
		this.walk = walk;
		this.startCommit = start;
	}

	/**
	 * @return the starting commit of this walk
	 */
	public Commit getStart() {
		return startCommit;
	}

	/**
	 * Loads all parents of this commit and returns them in a collection.
	 *
	 * @param child the child commit whose parents are loaded
	 * @return a collection of parent commits of the specified child commit
	 */
	Collection<Commit> getParents(Commit child) throws IOException {
		Collection<CommitHash> parentHashes = child.getParentHashes();
		Collection<Commit> parents = new ArrayList<>(parentHashes.size());

		for (CommitHash parentHash : parentHashes) {
			ObjectId commitPtr = repo.resolve(parentHash.getHash());
			RevCommit revCommit = walk.parseCommit(commitPtr);

			parents.add(commitAccess.commitFromRevCommit(repoId, revCommit));
		}

		return parents;
	}

	@Override
	public void close() {
		this.walk.close();
		this.repo.close();
	}

}
