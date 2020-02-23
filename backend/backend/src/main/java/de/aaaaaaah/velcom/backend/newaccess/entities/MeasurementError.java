package de.aaaaaaah.velcom.backend.newaccess.entities;

import java.util.Objects;

/**
 * This class represents a failed {@link Measurement}'s state, which only contains an message.
 */
public class MeasurementError {

	private final String errorMessage;

	public MeasurementError(String errorMessage) {
		this.errorMessage = Objects.requireNonNull(errorMessage);
	}

	public String getErrorMessage() {
		return errorMessage;
	}

}
