package de.aaaaaaah.velcom.backend.restapi.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

public class InvalidQueryParamsExceptionMapper implements
	ExceptionMapper<InvalidQueryParamsException> {

	@Override
	public Response toResponse(InvalidQueryParamsException exception) {
		return Response
			.status(Status.FORBIDDEN)
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
