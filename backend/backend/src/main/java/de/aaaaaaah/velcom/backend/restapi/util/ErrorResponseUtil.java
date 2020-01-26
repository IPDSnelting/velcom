package de.aaaaaaah.velcom.backend.restapi.util;

import de.aaaaaaah.velcom.backend.restapi.jsonobjects.JsonError;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Contains some helpers for dealing with error responses.
 */
public final class ErrorResponseUtil {

	private ErrorResponseUtil() {
		throw new UnsupportedOperationException("No instantiation");
	}

	/**
	 * Returns a valid error response.
	 *
	 * @param status the status code
	 * @param message the message to send
	 * @return the build response
	 */
	public static Response errorResponse(Status status, String message) {
		return Response.status(status)
			.entity(new JsonError(message))
			.build();
	}

	/**
	 * Constructs an error response and throws a {@link ClientErrorException} returning it to the
	 * server.
	 *
	 * @param status the status code
	 * @param message the message to send
	 * @throws ClientErrorException the created exception
	 */
	public static void throwErrorResponse(Status status, String message)
		throws ClientErrorException {
		throw new ClientErrorException(errorResponse(status, message));
	}
}
