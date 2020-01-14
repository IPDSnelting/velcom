package de.aaaaaaah.velcom.backend.access.commit.filter;

import java.io.IOException;
import java.time.Instant;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

class Before extends AuthorTimeRevFilter {

	private final Instant start;

	public Before(Instant start) {
		this.start = start;
	}

	@Override
	public boolean include(RevWalk walker, RevCommit cmit)
		throws StopWalkException, MissingObjectException, IncorrectObjectTypeException, IOException {

		return cmit.getAuthorIdent().getWhen().toInstant().isAfter(start);
	}
}
