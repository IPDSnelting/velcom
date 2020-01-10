package de.aaaaaaah.velcom.backend.access.benchmark;

import java.util.NoSuchElementException;

/**
 * This exception is thrown whenever an invalid {@link RunId} is used.
 */
public class NoSuchRunException extends NoSuchElementException {

	private final RunId runId;

	public NoSuchRunException(RunId runId) {
		this.runId = runId;
	}

	public RunId getRunId() {
		return runId;
	}

	@Override
	public String getMessage() {
		return "no run " + runId;
	}

}
