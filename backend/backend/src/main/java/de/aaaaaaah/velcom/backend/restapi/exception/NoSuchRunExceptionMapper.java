package de.aaaaaaah.velcom.backend.restapi.exception;

import de.aaaaaaah.velcom.backend.access.exceptions.NoSuchRunException;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An {@link ExceptionMapper} that transforms {@link NoSuchRunException}s to NOT_FOUND.
 */
public class NoSuchRunExceptionMapper implements ExceptionMapper<NoSuchRunException> {

	@Override
	public Response toResponse(NoSuchRunException exception) {
		return Response
			.status(Status.NOT_FOUND)
			.entity(new Info(
				"could not find run",
				exception.getInvalidId().getId()
			))
			.build();
	}

	private static class Info {

		private final String message;
		private final UUID id;

		public Info(String message, UUID id) {
			this.message = message;
			this.id = id;
		}

		public String getMessage() {
			return message;
		}

		public UUID getId() {
			return id;
		}
	}
}