package de.aaaaaaah.velcom.backend.restapi.endpoints.utils;

/**
 * A query parameter could not be parsed correctly.
 */
public class ArgumentParseException extends RuntimeException {

	public ArgumentParseException(String message) {
		super(message);
	}
}
