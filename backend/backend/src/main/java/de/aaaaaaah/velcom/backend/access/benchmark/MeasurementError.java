package de.aaaaaaah.velcom.backend.access.benchmark;

/**
 * This class represents a failed {@link Measurement}'s state, which only contains an message.
 */
public class MeasurementError {

	private final String errorMessage;

	MeasurementError(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
