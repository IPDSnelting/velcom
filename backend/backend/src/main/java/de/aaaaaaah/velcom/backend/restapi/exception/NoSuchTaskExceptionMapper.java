package de.aaaaaaah.velcom.backend.restapi.exception;

import de.aaaaaaah.velcom.backend.newaccess.taskaccess.exceptions.NoSuchTaskException;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An {@link ExceptionMapper} that transforms {@link NoSuchTaskException}s to NOT_FOUND.
 */
public class NoSuchTaskExceptionMapper implements ExceptionMapper<NoSuchTaskException> {

	@Override
	public Response toResponse(NoSuchTaskException exception) {
		return Response
			.status(Status.NOT_FOUND)
			.entity(new Info(
				"could not find task",
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
