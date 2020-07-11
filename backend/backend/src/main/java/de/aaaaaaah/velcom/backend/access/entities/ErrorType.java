package de.aaaaaaah.velcom.backend.access.entities;

/**
 * Describes which type of error has occurred when trying to perform a benchmark run.
 */
public enum ErrorType {

	/**
	 * The benchmark script returned a global error.
	 */
	BENCH_SCRIPT_ERROR("BENCH"),

	/**
	 * Some error occured in velcom which prevented the execution of the benchmark script.
	 */
	VELCOM_ERROR("VELCOM");

	private final String textualRepresentation;

	ErrorType(String textualRepresentation) {
		this.textualRepresentation = textualRepresentation;
	}

	public String getTextualRepresentation() {
		return textualRepresentation;
	}

	/**
	 * Tries to find the error type that matches the given string representation.
	 *
	 * @param representation the representation
	 * @return the error type that matches the given string representation
	 * @throws IllegalArgumentException if no error type matches the given string representation
	 */
	public static ErrorType fromTextualRepresentation(String representation)
		throws IllegalArgumentException {

		for (ErrorType errorType : ErrorType.values()) {
			if (errorType.getTextualRepresentation().equals(representation)) {
				return errorType;
			}
		}

		throw new IllegalArgumentException("\"" + representation + "\" is not a valid error type");
	}

}
