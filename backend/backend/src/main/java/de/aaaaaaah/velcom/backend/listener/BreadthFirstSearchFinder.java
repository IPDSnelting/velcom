package de.aaaaaaah.velcom.backend.listener;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.commit.CommitWalk;
import de.aaaaaaah.velcom.backend.access.repo.Branch;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * A breadth first search implementation of {@link UnknownCommitFinder}.
 */
public class BreadthFirstSearchFinder implements UnknownCommitFinder {

	@Override
	public Collection<Commit> find(CommitAccess commitAccess, Branch branch) throws IOException {
		try (CommitWalk walk = commitAccess.getCommitWalk(branch)) {
			ArrayList<Commit> unknownCommits = new ArrayList<>();
			HashSet<CommitHash> visitedCommits = new HashSet<>();

			LinkedList<Commit> commitQueue = new LinkedList<>();
			commitQueue.add(walk.getStart());

			while (!commitQueue.isEmpty()) {
				Commit current = commitQueue.poll();

				if (visitedCommits.contains(current.getHash())) {
					// current commit was already visited within the context
					// of this breadth first search
					continue;
				} else {
					visitedCommits.add(current.getHash());
				}

				if (current.isKnown()) {
					// Since this commit is known, all parents of this commit
					// are known as well => skip this one
					continue;
				} else {
					unknownCommits.add(current);
					commitQueue.addAll(walk.getParents(current));
				}
			}

			return unknownCommits;
		}
	}

}
