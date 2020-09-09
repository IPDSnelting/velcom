package de.aaaaaaah.velcom.backend.restapi.exception;

import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * An {@link ExceptionMapper} that transforms {@link TaskAlreadyExistsException}s to CONFLICT.
 */
public class TaskAlreadyExistsExceptionMapper implements
	ExceptionMapper<TaskAlreadyExistsException> {

	@Override
	public Response toResponse(TaskAlreadyExistsException exception) {
		return Response
			.status(Status.CONFLICT)
			.entity(new Info(
				"that task already exists in the queue",
				exception.getRepoId().getId(),
				exception.getHash().getHash()
			))
			.build();
	}

	private static class Info {

		private final String message;
		private final UUID repoId;
		private final String hash;

		public Info(String message, UUID repoId, String hash) {
			this.message = message;
			this.repoId = repoId;
			this.hash = hash;
		}

		public String getMessage() {
			return message;
		}

		public UUID getRepoId() {
			return repoId;
		}

		public String getHash() {
			return hash;
		}
	}
}
