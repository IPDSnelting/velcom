package de.aaaaaaah.velcom.backend.restapi.exception;

import de.aaaaaaah.velcom.backend.restapi.endpoints.utils.ArgumentParseException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An {@link ExceptionMapper} that transforms {@link ArgumentParseException}s to BAD_REQUEST.
 */
public class ArgumentParseExceptionMapper implements ExceptionMapper<ArgumentParseException> {

	@Override
	public Response toResponse(ArgumentParseException exception) {
		return Response
			.status(Status.BAD_REQUEST)
			.entity(new Info(exception.getMessage()))
			.build();
	}

	private static class Info {

		private final String message;

		public Info(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}
	}
}
