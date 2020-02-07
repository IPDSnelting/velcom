package de.aaaaaaah.velcom.backend.newaccess.entities;

import java.util.List;

/**
 * Describes how a measurement value should be interpreted.
 */
public enum Interpretation {

	/**
	 * The measurement value can be better or worse compared to any other measurement value
	 * regardless of whether it is greater than or lesser than the other measurement value.
	 */
	NEUTRAL("NEUTRAL"),

	/**
	 * A measurement value is better than another if its value is lesser than the other's value.
	 */
	LESS_IS_BETTER("LESS_IS_BETTER"),

	/**
	 * A measurement value is better than another if its value is greater than the other's value.
	 */
	MORE_IS_BETTER("MORE_IS_BETTER");

	private final String textualRepresentation;

	Interpretation(String textualRepresentation) {
		this.textualRepresentation = textualRepresentation;
	}

	/**
	 * Try to interpret a string as the textual representation of an {@link Interpretation}.
	 *
	 * @param representation the string to interpret as {@link Interpretation}
	 * @return the resulting {@link Interpretation}, if any could be found
	 * @throws IllegalArgumentException if the string does not correspond to any {@link
	 *                                  Interpretation}'s textual representation
	 */
	public static Interpretation fromTextualRepresentation(String representation)
		throws IllegalArgumentException {

		for (Interpretation interpretation : List.of(NEUTRAL, LESS_IS_BETTER, MORE_IS_BETTER)) {
			if (interpretation.getTextualRepresentation().equals(representation)) {
				return interpretation;
			}
		}

		throw new IllegalArgumentException(
			"\"" + representation + "\" is not a valid interpretation");
	}

	public String getTextualRepresentation() {
		return textualRepresentation;
	}
}
