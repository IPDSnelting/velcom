package de.aaaaaaah.velcom.backend.access.commit.filter;

import java.io.IOException;
import java.time.Instant;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

class After extends AuthorTimeRevFilter {

	private final Instant stop;

	public After(Instant stop) {
		this.stop = stop;
	}

	@Override
	public boolean include(RevWalk walker, RevCommit cmit)
		throws StopWalkException, MissingObjectException, IncorrectObjectTypeException, IOException {

		return cmit.getAuthorIdent().getWhen().toInstant().isBefore(stop);
	}
}
