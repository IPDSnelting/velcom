package de.aaaaaaah.velcom.backend.listener;

import de.aaaaaaah.velcom.backend.access.CommitWalk;
import de.aaaaaaah.velcom.backend.access.KnownCommitReadAccess;
import de.aaaaaaah.velcom.backend.access.entities.Commit;
import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.newaccess.committaccess.CommitReadAccess;
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
	public Collection<Commit> find(CommitReadAccess commitAccess, KnownCommitReadAccess knownAccess,
		Commit start) throws IOException {

		try (CommitWalk walk = commitAccess.getCommitWalk(start)) {
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

				if (knownAccess.isKnown(start.getRepoId(), current.getHash())) {
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
