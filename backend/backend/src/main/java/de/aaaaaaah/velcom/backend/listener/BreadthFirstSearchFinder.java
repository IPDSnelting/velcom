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
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * A breadth first search implementation of {@link UnknownCommitFinder}.
 */
public class BreadthFirstSearchFinder implements UnknownCommitFinder {

	private static final int MAX_COMMITS = 1000;

	@Override
	public Collection<Commit> find(CommitAccess commitAccess, Branch branch) throws IOException {
		try (CommitWalk walk = commitAccess.getCommitWalk(branch)) {
			List<Commit> unknownCommits = new ArrayList<>();
			Set<CommitHash> visitedCommits = new HashSet<>();

			Queue<Commit> commitQueue = new LinkedList<>();
			commitQueue.add(walk.getStart());

			while (!commitQueue.isEmpty()) {
				if (unknownCommits.size() >= MAX_COMMITS) {
					break;
				}

				Commit current = commitQueue.poll();

				if (!visitedCommits.add(current.getHash())) {
					// current commit was already visited within the context
					// of this breadth first search
					continue;
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
