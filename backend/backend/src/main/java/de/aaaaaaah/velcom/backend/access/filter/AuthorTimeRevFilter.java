package de.aaaaaaah.velcom.backend.access.filter;

import java.time.Instant;
import org.eclipse.jgit.revwalk.filter.RevFilter;

public abstract class AuthorTimeRevFilter extends RevFilter {

	public static AuthorTimeRevFilter before(Instant time) {
		return new Before(time);
	}

	public static AuthorTimeRevFilter after(Instant time) {
		return new After(time);
	}

	public static AuthorTimeRevFilter between(Instant start, Instant stop) {
		return new Between(start, stop);
	}

	@Override
	public RevFilter clone() {
		return this; // I am immutable!
	}
}
