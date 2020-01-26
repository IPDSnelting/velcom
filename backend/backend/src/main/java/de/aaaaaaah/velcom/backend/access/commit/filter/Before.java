package de.aaaaaaah.velcom.backend.access.commit.filter;

import java.io.IOException;
import java.time.Instant;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

class Before extends AuthorTimeRevFilter {

	private final Instant time;

	public Before(Instant time) {
		this.time = time;
	}

	@Override
	public boolean include(RevWalk walker, RevCommit cmit)
		throws StopWalkException {

		return cmit.getAuthorIdent().getWhen().toInstant().isBefore(time);
	}
}
