package de.aaaaaaah.velcom.backend.listener;

import de.aaaaaaah.velcom.backend.access.commit.Commit;
import de.aaaaaaah.velcom.backend.access.commit.CommitAccess;
import de.aaaaaaah.velcom.backend.access.commit.CommitHash;
import de.aaaaaaah.velcom.backend.access.commit.CommitWalk;
import de.aaaaaaah.velcom.backend.access.repo.Branch;
import de.aaaaaaah.velcom.backend.access.repo.RepoId;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * A breadth first search implementation of {@link UnknownCommitFinder}.
 */
public class BatchedBreadthFirstSearchFinder implements UnknownCommitFinder {

	private static final int BATCH_SIZE = 100;
	private static final int MAX_COMMITS = 1000;

	@Override
	public Collection<Commit> find(CommitAccess commitAccess, Branch branch) throws IOException {
		try (CommitWalk walk = commitAccess.getCommitWalk(branch)) {
			Set<CommitHash> visitedCommits = new HashSet<>();

			Queue<Commit> commitQueue = new LinkedList<>();
			commitQueue.add(walk.getStart());

			Set<Commit> pendingCommits = new HashSet<>();
			Set<Commit> knownCommits = new HashSet<>();
			Set<Commit> unknownCommits = new HashSet<>();

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

				pendingCommits.add(current);

				if (pendingCommits.size() >= BATCH_SIZE) {
					checkBatch(commitAccess, branch.getRepoId(), pendingCommits, knownCommits,
						unknownCommits);
					pendingCommits.clear();
				}

				if (knownCommits.contains(current)) {
					// Since this commit is known, all parents of this commit
					// are known as well => skip this one
					continue;
				} else if (unknownCommits.contains(current)) {
					// this commit either unknown or it has not been checked yet
					// regardless, check all its parents
					commitQueue.addAll(walk.getParents(current));
				}
			}

			return unknownCommits;
		}
	}

	private void checkBatch(CommitAccess access, RepoId repoId, Set<Commit> pending,
		Set<Commit> known, Set<Commit> unknown) {

		final Set<CommitHash> knownHashes = access.getKnownCommits(repoId, pending);

		for (Commit commit : pending) {
			if (knownHashes.contains(commit.getHash())) {
				known.add(commit);
			} else {
				unknown.add(commit);
			}
		}
	}

}
