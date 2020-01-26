package de.aaaaaaah.velcom.backend.access.commit.filter;

import java.time.Instant;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

class After extends AuthorTimeRevFilter {

	private final Instant time;

	public After(Instant time) {
		this.time = time;
	}

	@Override
	public boolean include(RevWalk walker, RevCommit cmit)
		throws StopWalkException {

		return cmit.getAuthorIdent().getWhen().toInstant().isAfter(time);
	}
}
