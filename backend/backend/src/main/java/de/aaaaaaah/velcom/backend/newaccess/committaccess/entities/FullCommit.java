package de.aaaaaaah.velcom.backend.newaccess.committaccess.entities;

import de.aaaaaaah.velcom.backend.access.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.entities.RepoId;
import java.time.Instant;
import java.util.Set;

public class FullCommit extends Commit {

	private final Set<CommitHash> parentHashes;
	private final Set<CommitHash> childHashes;

	public FullCommit(RepoId repoId, CommitHash hash, boolean tracked, String author,
		Instant authorDate, String committer, Instant committerDate, String message,
		Set<CommitHash> parentHashes, Set<CommitHash> childHashes) {

		super(repoId, hash, tracked, author, authorDate, committer, committerDate, message);

		this.parentHashes = parentHashes;
		this.childHashes = childHashes;
	}

	public Set<CommitHash> getParentHashes() {
		return parentHashes;
	}

	public Set<CommitHash> getChildHashes() {
		return childHashes;
	}
}
