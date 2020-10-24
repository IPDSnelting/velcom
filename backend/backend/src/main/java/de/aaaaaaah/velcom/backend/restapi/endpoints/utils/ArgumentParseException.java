package de.aaaaaaah.velcom.backend.restapi.endpoints.utils;

/**
 * A query parameter could not be parsed correctly.
 */
// TODO: 24/10/2020 Make more/less specific? Maybe even get rid of this?
public class ArgumentParseException extends RuntimeException {

	public ArgumentParseException(String message) {
		super(message);
	}
}
