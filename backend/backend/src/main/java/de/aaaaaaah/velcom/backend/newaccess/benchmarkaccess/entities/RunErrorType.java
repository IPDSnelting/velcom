package de.aaaaaaah.velcom.backend.newaccess.benchmarkaccess.entities;

/**
 * Describes which type of error has occurred when trying to perform a benchmark run.
 */
public enum RunErrorType {

	/**
	 * The benchmark script returned a global error.
	 */
	BENCH_SCRIPT_ERROR("BENCH"),

	/**
	 * Some error occurred in velcom which prevented the execution of the benchmark script.
	 */
	VELCOM_ERROR("VELCOM");

	private final String textualRepresentation;

	RunErrorType(String textualRepresentation) {
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
	public static RunErrorType fromTextualRepresentation(String representation)
		throws IllegalArgumentException {

		for (RunErrorType runErrorType : RunErrorType.values()) {
			if (runErrorType.getTextualRepresentation().equals(representation)) {
				return runErrorType;
			}
		}

		throw new IllegalArgumentException("\"" + representation + "\" is not a valid error type");
	}

}
