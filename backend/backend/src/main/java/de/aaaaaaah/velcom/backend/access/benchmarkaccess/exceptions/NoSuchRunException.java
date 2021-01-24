package de.aaaaaaah.velcom.backend.access.benchmarkaccess.exceptions;

import de.aaaaaaah.velcom.backend.access.benchmarkaccess.entities.RunId;
import de.aaaaaaah.velcom.backend.access.committaccess.entities.CommitHash;
import de.aaaaaaah.velcom.backend.access.repoaccess.entities.RepoId;
import de.aaaaaaah.velcom.shared.util.Either;
import de.aaaaaaah.velcom.shared.util.Pair;

/**
 * This exception is thrown whenever an invalid {@link RunId} is used.
 */
public class NoSuchRunException extends RuntimeException {

	private final Either<RunId, Pair<RepoId, CommitHash>> invalidSource;

	public NoSuchRunException(RunId invalidId) {
		super("no run with id " + invalidId);
		invalidSource = Either.ofLeft(invalidId);
	}

	public NoSuchRunException(RepoId invalidRepoId, CommitHash invalidCommitHash) {
		super("no run for commit " + invalidCommitHash + " in repo " + invalidRepoId);
		invalidSource = Either.ofRight(new Pair<>(invalidRepoId, invalidCommitHash));
	}

	public Either<RunId, Pair<RepoId, CommitHash>> getInvalidSource() {
		return invalidSource;
	}
}
