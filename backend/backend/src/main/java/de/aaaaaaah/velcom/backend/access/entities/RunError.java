package de.aaaaaaah.velcom.backend.access.entities;

import java.util.Objects;

/**
 * Occurs when either the benchmark script cause a global error or the runner itself fails to
 * execute the benchmark script.
 */
public class RunError {

	private final String errorMessage;
	private final RunErrorType type;

	public RunError(String errorMessage, RunErrorType type) {
		this.errorMessage = Objects.requireNonNull(errorMessage);
		this.type = Objects.requireNonNull(type);
	}

	public String getMessage() {
		return errorMessage;
	}

	public RunErrorType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "RunError{" +
			"errorMessage='" + errorMessage + '\'' +
			", type=" + type +
			'}';
	}

}
