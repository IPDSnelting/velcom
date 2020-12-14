package de.aaaaaaah.velcom.backend.restapi.exception;

/**
 * An exception thrown when an endpoint's query parameters are invalid.
 */
public class InvalidQueryParamsException extends RuntimeException {

	public InvalidQueryParamsException(String message) {
		super(message);
	}
}
