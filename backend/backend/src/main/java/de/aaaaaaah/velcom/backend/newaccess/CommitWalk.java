package de.aaaaaaah.velcom.backend.newaccess;


import de.aaaaaaah.velcom.backend.newaccess.entities.Commit;
import de.aaaaaaah.velcom.backend.newaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.entities.RepoId;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public class CommitWalk implements AutoCloseable {

	private final RepoId repoId;
	private final Repository repo;
	private final RevWalk walk;
	private final Commit startCommit;

	public CommitWalk(RepoId repoId, Repository repo, RevWalk walk, Commit startCommit) {
		this.repoId = repoId;
		this.repo = repo;
		this.walk = walk;
		this.startCommit = startCommit;
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
	 * @throws IOException if an exception occurs trying to load the parents
	 */
	public Collection<Commit> getParents(Commit child) throws IOException {
		Collection<CommitHash> parentHashes = child.getParentHashes();
		Collection<Commit> parents = new ArrayList<>(parentHashes.size());

		for (CommitHash parentHash : parentHashes) {
			ObjectId commitPtr = repo.resolve(parentHash.getHash());
			RevCommit revCommit = walk.parseCommit(commitPtr);

			parents.add(CommitReadAccess.commitFromRevCommit(repoId, revCommit));
		}

		return parents;
	}

	@Override
	public void close() {
		walk.close();
		repo.close();
	}

}
