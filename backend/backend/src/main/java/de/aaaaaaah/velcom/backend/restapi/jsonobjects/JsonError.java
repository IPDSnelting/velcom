package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

/**
 * An error response.
 */
public class JsonError {

	private final String error;

	public JsonError(String error) {
		this.error = error;
	}

	public String getError() {
		return error;
	}
}
