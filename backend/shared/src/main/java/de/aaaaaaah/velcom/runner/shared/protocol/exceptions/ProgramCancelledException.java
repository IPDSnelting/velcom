package de.aaaaaaah.velcom.runner.shared.protocol.exceptions;

/**
 * A program was cancelled for a given reason.
 */
public class ProgramCancelledException extends RuntimeException {

	private final String reason;

	public ProgramCancelledException(String reason) {
		super(reason);
		this.reason = reason;
	}

	/**
	 * Returns the reason.
	 *
	 * @return the reason
	 */
	public String getReason() {
		return reason;
	}

}
