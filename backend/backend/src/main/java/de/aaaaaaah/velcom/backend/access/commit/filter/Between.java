package de.aaaaaaah.velcom.backend.access.commit.filter;

import java.time.Instant;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

class Between extends AuthorTimeRevFilter {

	private final Instant start;
	private final Instant stop;

	public Between(Instant start, Instant stop) {
		this.start = start;
		this.stop = stop;
	}

	@Override
	public boolean include(RevWalk walker, RevCommit cmit)
		throws StopWalkException {

		final Instant authorTime = cmit.getAuthorIdent().getWhen().toInstant();
		return authorTime.isAfter(start) && authorTime.isBefore(stop);
	}
}
