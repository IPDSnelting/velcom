package de.aaaaaaah.velcom.backend.access.entities;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MeasurementError that = (MeasurementError) o;
		return errorMessage.equals(that.errorMessage);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errorMessage);
	}

	@Override
	public String toString() {
		return "MeasurementError{" +
			"errorMessage='" + errorMessage + '\'' +
			'}';
	}

}
