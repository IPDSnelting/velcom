package de.aaaaaaah.velcom.backend.access.commit.filter;

import java.time.Instant;
import org.eclipse.jgit.revwalk.filter.RevFilter;

public abstract class AuthorTimeRevFilter extends RevFilter {

	public static AuthorTimeRevFilter before(Instant start) {
		return new Before(start);
	}

	public static AuthorTimeRevFilter after(Instant stop) {
		return new After(stop);
	}

	public static AuthorTimeRevFilter between(Instant start, Instant stop) {
		return new Between(start, stop);
	}

	@Override
	public boolean requiresCommitBody() {
		return true; // TODO test if this also works with false
	}

	@Override
	public RevFilter clone() {
		return this; // I am immutable!
	}
}
